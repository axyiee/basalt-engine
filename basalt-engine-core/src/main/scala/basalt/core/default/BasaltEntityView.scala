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

import basalt.core.datatype.{Entity, monoidForEntity}
import basalt.core.engine.{Engine, EntityView}
import cats.{Applicative, Monoid}
import cats.syntax.all._

class BasaltEntityView[F[_]: Applicative](engine: BasaltEngine[F])
    extends EntityView[F] {
  given Engine[F] = engine

  def create: F[Entity[F]] = Applicative[F].pure(Monoid[Entity[F]].empty)

  def delete(id: Long): F[Unit] = Applicative[F].unit
}
