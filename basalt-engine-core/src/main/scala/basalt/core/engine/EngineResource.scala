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
package basalt.core.engine

/** A resource is a unique instance of a data type stored independent from
  * entities. Those are available in all contexts, to every entity, and are used
  * for storing global context or data that is not tied to any specific entity,
  * such as configurations or game states.
  *
  * ==Example==
  *
  * {{{
  *  object Config extends EngineResource[Config.Data]:
  *     case class Data(...)
  *
  *   object GameState extends EngineResource[GameState.State]:
  *     enum State:
  *       case Menu, Playing, Paused, GameOver
  * }}}
  *
  * @tparam A
  *   the type of the resource.
  */
trait EngineResource[A]:
  def access[F[_]](using Engine[F]): F[A]
