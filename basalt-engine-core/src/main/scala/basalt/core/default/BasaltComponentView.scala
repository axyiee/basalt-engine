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

import basalt.core.datatype.Component
import basalt.core.engine.{ComponentView, Engine}
import basalt.core.query.QueryingFilterTag
import cats.Monoid
import cats.syntax.all._
import cats.effect.kernel.Sync

class BasaltComponentView[F[_]: Sync](engine: BasaltEngine[F])
    extends ComponentView[F] {
  given Engine[F] = engine

  def getId[C <: Component: QueryingFilterTag](using
      tag: QueryingFilterTag[C]
  ): Option[Int] = engine.componentIds.get(tag.qualifiedName)

  def extract[C <: Component: QueryingFilterTag](
      targetEntityId: Long
  ): Either[IllegalArgumentException, Option[C]] =
    engine.entityIndex
      .get(targetEntityId)
      .toRight(
        IllegalArgumentException(s"Entity $targetEntityId does not exist")
      )
      .map(
        engine.archetypeIndex
          .get(_)
          .flatMap(archetype => archetype.getComponent[C](targetEntityId))
      )

  def remove[C <: Component: QueryingFilterTag](
      targetEntityId: Long
  ): F[Option[C]] = Sync[F].pure(Option.empty[C])

  def update[C <: Component: QueryingFilterTag](
      targetEntityId: Long,
      content: C
  ): F[Unit] = Sync[F].unit
}
