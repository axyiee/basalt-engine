/**
 * Basalt Engine, an open-source ECS engine for Scala 3
 * Copyright (C) 2023 Pedro Henrique
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package basalt.core.default

import basalt.core.archetype.ComponentArchetype
import basalt.core.datatype.{Component, EntityId}
import basalt.core.descriptor.{
  ArchetypesDescriptor,
  ComponentsDescriptor,
  EntitiesDescriptor
}
import basalt.core.engine.ComponentView
import basalt.core.event.internal.InternalEvent
import basalt.core.query.{ComponentFilterTag, QueryingFilterTag}

import cats.Monoid
import cats.effect.kernel.{Async, Sync}
import cats.syntax.all._

import collection.mutable.LongMap

import fs2.Stream
import fs2.concurrent.Topic
import basalt.core.event.internal.{
  ComponentAdded,
  ComponentUpdated,
  ComponentRemoved
}

class BasaltComponentView[F[_]: Sync](
    val descriptor: ComponentsDescriptor[F],
    val entities: EntitiesDescriptor[F],
    val archetypes: ArchetypesDescriptor[F],
    val events: Topic[F, InternalEvent]
) extends ComponentView[F]:
  override def getId[C <: Component: ComponentFilterTag]: F[Long] =
    descriptor.getId[C]

  override def extract[C <: Component: ComponentFilterTag](
      entityId: EntityId
  ): F[C] =
    for
      componentId <- getId[C]
      archetypeId <- entities.liftArchetypeId(entityId)
      archetype   <- archetypes.lift(archetypeId)
      component   <- archetype.liftComponent[C](entityId, componentId)
    yield component

  override def extractAll(
      entityId: EntityId
  ): Stream[F, Component] =
    for
      archetypeId <- Stream.eval(entities.liftArchetypeId(entityId))
      archetype   <- Stream.eval(archetypes.lift(archetypeId))
      components  <- Stream.emits(archetype.getComponents(entityId).toSeq)
    yield components

  override def remove[C <: Component: ComponentFilterTag](
      entityId: EntityId
  ): F[Unit] =
    for
      componentId  <- getId[C]
      archetypeId  <- entities.liftArchetypeId(entityId)
      archetype    <- archetypes.lift(archetypeId)
      newArchetype <- archetype.removeComponent(entityId, componentId)
      _            <- entities.setArchetypeId(entityId, newArchetype.id)
      _ <-
        if newArchetype.id == archetypeId then Sync[F].unit
        else
          events
            .publish1(ComponentRemoved(entityId, componentId))
            .flatMap(_ match
              case Right(_) => Sync[F].unit
              case Left(_) =>
                Sync[F].raiseError(
                  new IllegalStateException(
                    s"Could not publish event for component removal of entity $entityId. Event publisher is closed."
                  )
                )
            )
    yield ()

  override def set[C <: Component: ComponentFilterTag](
      entityId: EntityId,
      content: C
  ): F[Unit] =
    for
      componentId  <- getId[C]
      archetypeId  <- entities.liftArchetypeId(entityId)
      archetype    <- archetypes.lift(archetypeId)
      newArchetype <- archetype.addComponent(entityId, componentId, content)
      _            <- entities.setArchetypeId(entityId, newArchetype.id)
      _ <-
        val event =
          if newArchetype.id == archetypeId then
            ComponentUpdated(entityId, componentId)
          else ComponentAdded(entityId, componentId)
        events
          .publish1(event)
          .flatMap(_ match
            case Right(_) => Sync[F].unit
            case Left(_) =>
              Sync[F].raiseError(
                new IllegalStateException(
                  s"Could not publish event for component update/add of entity $entityId. Event publisher is closed."
                )
              )
          )
    yield ()
