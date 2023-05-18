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
import basalt.core.query.{QueryingFilterIterable, QueryingFilterIterableTag}

/** A [System] is a function which can modify content within an engine.
  *
  * @tparam F
  *   the effect type used for the engine.
  * @tparam C
  *   the context for the current execution.
  */
trait System[F[_], C] {
  def execute(context: C, engine: Engine[F]): F[Unit]
}

/** A [System] is a function which matches a specific querying context related
  * to entities and components, which can modify content within an engine.
  *
  * @tparam F
  *   the effect type used for the engine.
  * @tparam Q
  *   the querying condition for this system to be run.
  */
trait IteratingSystem[F[_], Q <: QueryingFilterIterable, C] extends System[F, C]
