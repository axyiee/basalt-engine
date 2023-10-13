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
package basalt.core.datatype

import basalt.core.engine.{ComponentView, Engine}
import basalt.core.query.{QueryingFilterIterable, QueryingFilterIterableTag}
import basalt.core.query.QueryingFilterTag
import basalt.core.query.filters.Added
import cats.effect.kernel.{Spawn, Sync}
import cats.syntax.all._
import cats.effect.implicits._
import fs2.Stream
import cats.Applicative

/** A [[System]] is a function which can modify content within an engine.
  *
  * @tparam F
  *   the effect type used for the engine.
  */
trait System[F[_]]:
  def execute(tick: Tick, engine: Engine[F]): Stream[F, Unit]

/** An [[IteratingSystem]] is a function which matches a specific context
  * related to entities and components, which can modify content within an
  * engine and adds functionality/behaviour to it.
  *
  * @tparam F
  *   the effect type used for the engine.
  * @tparam Q
  *   the querying condition for this system to be run.
  */
trait IteratingSystem[F[
    _
]: Sync, I <: QueryingFilterIterable: QueryingFilterIterableTag]
    extends System[F]:
  def update(entity: EntityRef[F], query: I)(
      tick: Tick,
      engine: Engine[F]
  ): F[Unit]
  final def execute(tick: Tick, engine: Engine[F]): Stream[F, Unit] =
    val context = engine.components.extractWithinContext[I]
    context.flatMap { case (entity, query) =>
      engine.entities.get(entity) match
        case Right(entity) => Stream.eval(update(entity, query)(tick, engine))
        case Left(error)   => Stream.raiseError(error)
    }

