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

import basalt.core.datatype.{EntityId, EntityRef}
import basalt.core.descriptor.{ArchetypesDescriptor, EntitiesDescriptor}
import basalt.core.engine.{Engine, EntityView}

import cats.{Applicative, Monoid}
import cats.effect.kernel.Sync
import cats.syntax.all._

import collection.mutable.LongMap
import basalt.core.descriptor.ComponentsDescriptor
import basalt.core.engine.ComponentView
import basalt.core.archetype.ArchetypeId

class BasaltEntityView[F[_]: Sync](
    val descriptor: EntitiesDescriptor[F],
    val archetypes: ArchetypesDescriptor[F],
    val components: ComponentView[F]
) extends EntityView[F]:
  override def create: F[EntityRef[F]] =
    descriptor.init.map(EntityRef[F](_, this, components))

  override def delete(id: EntityId): F[Unit] =
    descriptor.kill(id, archetypes)

  override def get(id: EntityId): Either[NoSuchElementException, EntityRef[F]] =
    if descriptor.exists(id) then Right(EntityRef[F](id, this, components))
    else Left(new NoSuchElementException(s"Entity $id does not exist"))

  override def getArchetypeId(
      id: EntityId
  ): Either[NoSuchElementException, ArchetypeId] =
    descriptor
      .getArchetypeId(id)
      .toRight(
        new NoSuchElementException(s"Entity $id does not exist")
      )
