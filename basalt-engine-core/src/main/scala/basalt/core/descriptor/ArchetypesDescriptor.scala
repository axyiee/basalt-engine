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
package basalt.core.descriptor

import basalt.core.archetype.{ArchetypeId, ComponentArchetype}
import basalt.core.collection.GenerationalVector
import basalt.core.datatype.{ComponentSet, EntityId}

import collection.mutable.{LongMap, HashMap}

import cats.effect.kernel.Sync
import cats.effect.std.AtomicCell
import cats.syntax.all._
import cats.effect.kernel.Async

/** Information required by the ongoing ticking and querying processes,
  * available at runtime execution, dynamically created by an
  * [[basalt.core.engine.Engine]].
  */
class ArchetypesDescriptor[F[_]: Sync](
    val counter: AtomicCell[F, ArchetypeId],
    val archetypes: LongMap[ComponentArchetype[F]],
    val byComponentSet: HashMap[ComponentSet, ComponentArchetype[F]]
):
  def init(components: ComponentSet): F[ComponentArchetype[F]] =
    byComponentSet
      .get(components)
      .fold(
        for
          id <- counter.getAndUpdate(_ + 1)
          archetype = ComponentArchetype[F](id, components, apply)
          _ <- Sync[F].delay(archetypes.put(id, archetype))
          _ <- Sync[F].delay(byComponentSet.put(components, archetype))
        yield archetype
      )(Sync[F].pure)

  def apply(components: ComponentSet): F[ComponentArchetype[F]] =
    byComponentSet.get(components) match
      case Some(archetype) => Sync[F].pure(archetype)
      case None            => init(components)

  def apply(id: ArchetypeId): Option[ComponentArchetype[F]] =
    archetypes.get(id)

  def switchFor(
      entity: EntityId,
      from: ArchetypeId,
      to: ArchetypeId
  ): F[Unit] =
    for
      archetypes <- Sync[F].pure((archetypes.get(from), archetypes.get(to)))
      _ <- archetypes match
        case (Some(fromArchetype), Some(toArchetype)) =>
          fromArchetype.moveEntity(toArchetype, entity)
        case _ => Sync[F].unit
    yield ()

  def removeEntity(entity: EntityId, archetype: ArchetypeId): F[Unit] =
    archetypes.get(archetype).fold(Sync[F].unit)(_.removeEntity(entity))

object ArchetypesDescriptor:
  def apply[F[_]: Async]: F[ArchetypesDescriptor[F]] =
    for
      counter    <- AtomicCell[F].of[ArchetypeId](1L)
      archetypes <- Sync[F].pure(LongMap.empty[ComponentArchetype[F]])
      byComponentSet <- Sync[F].pure(
        HashMap.empty[ComponentSet, ComponentArchetype[F]]
      )
    yield new ArchetypesDescriptor[F](counter, archetypes, byComponentSet)
