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
package basalt.core.datatype

import basalt.core.engine.Engine
import basalt.core.query.{QueryingFilter, QueryingFilterTag}
import cats.kernel.Monoid
import cats.Applicative
import cats.Id

/** An [[Entity]] is an identifier for a set of [[Component]]s on a specific
  * [[Engine]] context.
  *
  * Each [[Entity]] has an unique identification and a set of [[Component]]s and
  * [[Attribute]]s. Each [[Component]] must be associated with a type used up to
  * a single time on each [[Entity]].
  *
  * @tparam F
  *   the effect type used for the engine.
  */
trait Entity[F[_]]:
  def identity: cats.Id[Long]
  def insert[A <: Component: QueryingFilterTag, D](component: D)(using
      Engine[F]
  ): F[Unit]
  def remove[A <: Component: QueryingFilterTag](using
      QueryingFilterTag[A]
  )(using Engine[F]): F[Unit]
  def get[A <: Component: QueryingFilterTag, T](using
      QueryingFilterTag[A]
  )(using Engine[F]): Option[A]
  def has[A <: Component: QueryingFilterTag](using
      QueryingFilterTag[QueryingFilter]
  )(using Engine[F]): Boolean

given monoidForEntity[F[_]: Applicative](using Engine[F]): Monoid[Entity[F]]
with
  def empty: Entity[F] = new Entity[F] {
    def identity: cats.Id[Long] = -1
    override def get[A <: Component: QueryingFilterTag, T](using
        QueryingFilterTag[A]
    )(using Engine[F]): Option[A] = Option.empty[A]
    override def has[A <: Component: QueryingFilterTag](using
        QueryingFilterTag[QueryingFilter]
    )(using Engine[F]): Boolean = false
    override def insert[A <: Component: QueryingFilterTag, D](
        component: D
    )(using Engine[F]): F[Unit] = Applicative[F].unit
    override def remove[A <: Component: QueryingFilterTag](using
        QueryingFilterTag[A]
    )(using Engine[F]): F[Unit] = Applicative[F].unit
  }
  def combine(x: Entity[F], y: Entity[F]): Entity[F] =
    throw new UnsupportedOperationException("Cannot combine entities")
