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
package basalt.core.descriptor

import basalt.core.archetype.{ArchetypeId, ComponentArchetype}

import scala.collection.mutable.LongMap
import basalt.core.collection.GenerationalVector
import basalt.core.datatype.EntityId
import cats.effect.kernel.Sync

/** Information required by the ongoing ticking and querying processes,
  * available at runtime execution, dynamically created by an
  * [[basalt.core.engine.Engine]].
  */
class EntitiesDescriptor[F[_]: Sync](
    var meta: GenerationalVector[ArchetypeId] = GenerationalVector()
):
  def init: F[EntityId] =
    Sync[F].delay {
      val (key, vector) = meta.create(0)
      this.meta = vector
      key
    }

  def getArchetypeId(entity: EntityId): Option[ArchetypeId] =
    meta.get(entity).map(_.value)

  def switchArchetype(
      entity: EntityId,
      descriptor: ArchetypesDescriptor[F],
      to: ArchetypeId
  ): F[Unit] =
    Sync[F].delay {
      getArchetypeId(entity).foreach { from =>
        this.meta = meta.set(entity, to)
        descriptor.switchFor(entity, from, to)
      }
    }

  def kill(id: EntityId, archetypes: ArchetypesDescriptor[F]): F[Unit] =
    Sync[F].delay {
      getArchetypeId(id).foreach { archetype =>
        this.meta = meta.remove(id)
        archetypes.removeEntity(id, archetype)
      }
    }
