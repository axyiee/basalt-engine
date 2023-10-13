/** Basalt Engine, an open-source ECS engine for Scala 3 Copyright (C) 2023
  * Pedro Henrique
  *
  * This program is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as published by the
  * Free Software Foundation; either version 3 of the License, or (at your
  * option) any later version.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software Foundation,
  * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package basalt.core.default

import basalt.core.archetype.ComponentArchetype
import basalt.core.datatype.{Component, ComponentSet, EntityId}
import basalt.core.descriptor.{
  ArchetypesDescriptor,
  ComponentsDescriptor,
  EntitiesDescriptor
}
import basalt.core.engine.ComponentView
import basalt.core.syntax.all._
import basalt.core.syntax.all.given

import basalt.core.event.internal.{
  ComponentAdded,
  ComponentUpdated,
  ComponentRemoved,
  InternalEvent,
  InternalEvents
}
import basalt.core.query.{
  ComponentFilterTag,
  QueryingFilterTag,
  QueryingFilterIterable,
  QueryingFilterIterableTag
}
import basalt.core.query.filters.{Added, Changed, Removed}

import cats.Monoid
import cats.effect.kernel.{Async, Sync}
import cats.syntax.all._

import collection.mutable.LongMap
import util.boundary, boundary.break

import fs2.Stream
import basalt.core.query.Fin
import basalt.core.query.weakTypeTag
import basalt.core.query.QueryingFilter
import basalt.core.query.WeakTypeTag
import basalt.core.datatype.ComponentId
import basalt.core.archetype.ArchetypeId

class BasaltComponentView[F[_]: Sync](
    val descriptor: ComponentsDescriptor[F],
    val entities: EntitiesDescriptor[F],
    val archetypes: ArchetypesDescriptor[F],
    val events: InternalEvents[F]
) extends ComponentView[F]:
  private def isComponentPresentInArchetype(
      id: ComponentId,
      archetypeId: ArchetypeId
  ): Boolean =
    descriptor.componentIndex.get(id) match
      case Some(set) => set.contains(archetypeId)
      case _         => false

  override def getId[C <: Component: ComponentFilterTag]: F[Long] =
    descriptor.getId[C]

  override def makeComponentSet[I <: QueryingFilterIterable](using
      iter: QueryingFilterIterableTag[I]
  ): Either[IllegalArgumentException, ComponentSet] =
    boundary:
      var set = ComponentSet.empty
      for tag <- iter.tags do
        val inner = tag.inner.orElse(Some(tag))
        inner.foreach(tag =>
          descriptor.indices.get(tag.asInstanceOf) match
            case Some(id) => set = set + id
            case None =>
              break(Left(IllegalArgumentException("Unknown component type")))
        )
      Right(set)

  override def extractWithinContext[
      I <: QueryingFilterIterable: QueryingFilterIterableTag
  ](using
      iter: QueryingFilterIterableTag[I]
  ): Stream[F, (EntityId, I)] =
    val tagIds = iter.tags.flatMap(t => descriptor.indices.get(t.asInstanceOf))
    var matching = List.empty[ArchetypeId]
    for id <- tagIds do
    

  override def getArchetype[
      I <: QueryingFilterIterable: QueryingFilterIterableTag
  ](using
      iter: QueryingFilterIterableTag[I]
  ): Option[ComponentArchetype[F]] =
    val set = makeComponentSet[I](using iter) match
      case Left(error) => return None
      case Right(s)    => s
    archetypes.byComponentSet.get(set)

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
      _ <-
        if newArchetype.id == archetypeId then Sync[F].unit
        else
          entities
            .setArchetypeId(entityId, newArchetype.id)
            .flatTap(_ =>
              events.getAndUpdate(ComponentRemoved(entityId, componentId) :: _)
            )
            .as(())
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
      _ <- events.getAndUpdate(prev =>
        (if newArchetype.id == archetypeId then
           ComponentUpdated(entityId, componentId)
         else ComponentAdded(entityId, componentId))
          :: prev
      )
    yield ()
