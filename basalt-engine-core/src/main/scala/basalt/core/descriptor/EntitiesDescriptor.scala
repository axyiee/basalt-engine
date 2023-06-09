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
package basalt.core.descriptor

import basalt.core.archetype.{ArchetypeId, ComponentArchetype}
import basalt.core.collection.GenerationalVector
import basalt.core.datatype.EntityId

import cats.effect.kernel.Sync

import collection.mutable.{ArrayBuffer, LongMap}

/** Information required by the ongoing ticking and querying processes,
  * available at runtime execution, dynamically created by an
  * [[basalt.core.engine.Engine]].
  */
class EntitiesDescriptor[F[_]: Sync](
    val meta: GenerationalVector[ArchetypeId] = new GenerationalVector(
      ArrayBuffer.empty
    )
):
  def init: F[EntityId] =
    Sync[F].delay(meta.create(0))

  def getArchetypeId(entity: EntityId): Option[ArchetypeId] =
    meta.get(entity).map(_.value)

  def setArchetypeId(entity: EntityId, archetype: ArchetypeId): F[Unit] =
    Sync[F].delay(meta.set(entity, archetype))

  def switchArchetype(
      entity: EntityId,
      descriptor: ArchetypesDescriptor[F],
      to: ArchetypeId
  ): F[Unit] =
    Sync[F].delay {
      getArchetypeId(entity).foreach { from =>
        meta.set(entity, to)
        descriptor.switchFor(entity, from, to)
      }
    }

  def kill(id: EntityId, archetypes: ArchetypesDescriptor[F]): F[Unit] =
    Sync[F].delay {
      getArchetypeId(id).foreach { archetype =>
        meta.remove(id)
        archetypes.removeEntity(id, archetype)
      }
    }

  def exists(id: EntityId): Boolean =
    meta.contains(id)
