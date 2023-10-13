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
package basalt.core.query
package filters

import basalt.core.datatype.Component

/** Tracks the addition of a component to an entity.
  *
  * @tparam C
  *   the type of the component that has been added.
  */
trait Added[C <: Component: ComponentFilterTag] extends QueryingFilter

/** Tracks the removal of a component from an entity.
  *
  * @tparam C
  *   the type of the component that has been removed.
  */
trait Removed[C <: Component: ComponentFilterTag] extends QueryingFilter

/** Tracks the change of a component from an entity.
  *
  * @tparam C
  *   the type of the component that has been changed.
  */
trait Changed[C <: Component: ComponentFilterTag] extends QueryingFilter

