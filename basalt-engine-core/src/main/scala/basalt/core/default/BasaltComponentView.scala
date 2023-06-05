/**
 * Basalt Engine, a server-side modding engine for Minecraft third-party server implementations
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
import basalt.core.query.{ComponentFilterTag, QueryingFilterTag}

import cats.Monoid
import cats.effect.kernel.{Async, Sync}
import cats.syntax.all._

import collection.mutable.Stack
import scala.collection.mutable.LongMap

class BasaltComponentView[F[_]: Sync](
    val components: ComponentsDescriptor[F],
    val entities: EntitiesDescriptor[F],
    val archetypes: ArchetypesDescriptor[F]
) extends ComponentView[F]:
  override def getId[C <: Component: QueryingFilterTag]: F[Long] =
    components.getId[C]

  override def extract[C <: Component: QueryingFilterTag](
      target: EntityId
  ): F[C] =
    for
      componentId <- getId[C]
      archetypeId <- Sync[F].fromOption(
        entities.getArchetypeId(target),
        new IllegalArgumentException(s"Entity $target does not exist")
      )
      archetype <- Sync[F].fromOption(
        archetypes(archetypeId),
        new IllegalArgumentException(s"Archetype $archetypeId does not exist")
      )
      component <- Sync[F].fromOption(
        archetype.getComponent(target, componentId),
        new IllegalArgumentException(
          s"Component ${componentId} does not exist on entity $target"
        )
      )
    yield component.asInstanceOf[C]

  override def extractAll(
      target: EntityId
  ): fs2.Stream[F, Component] =
    for
      archetypeId <- fs2.Stream.fromOption(entities.getArchetypeId(target))
      archetype   <- fs2.Stream.fromOption(archetypes(archetypeId))
      component   <- fs2.Stream.emits(archetype.getComponents(target).toSeq)
    yield component

  override def remove[C <: Component: QueryingFilterTag](
      target: EntityId
  ): F[Unit] =
    for
      componentId <- getId[C]
      archetypeId <- Sync[F].fromOption(
        entities.getArchetypeId(target),
        new IllegalArgumentException(s"Entity $target does not exist")
      )
      archetype <- Sync[F].fromOption(
        archetypes(archetypeId),
        new IllegalArgumentException(s"Archetype $archetypeId does not exist")
      )
      component <- archetype.removeComponent(target, componentId)
    yield ()

  override def update[C <: Component: QueryingFilterTag](
      target: EntityId,
      content: C
  ): F[Unit] =
    for
      componentId <- getId[C]
      archetypeId <- Sync[F].fromOption(
        entities.getArchetypeId(target),
        new IllegalArgumentException(s"Entity $target does not exist")
      )
      archetype <- Sync[F].fromOption(
        archetypes(archetypeId),
        new IllegalArgumentException(s"Archetype $archetypeId does not exist")
      )
      component <- archetype.addComponent(target, componentId, content)
    yield ()
