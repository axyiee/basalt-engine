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
import cats.Id
import cats.kernel.Monoid

import scala.quoted.{Expr, Quotes, Type}

/** A [[Component]] is an unique data type without behaviour that can be
  * attached to an [[Entity]].
  *
  * Being unique means that an [[Entity]] can have up to a single component per
  * type.
  *
  * [[Component]] sare used to store data that is not provided by the platform
  * [[Attribute]]s; custom data given by the developer and used for extending
  * the functionality of an [[Entity]] or the [[Engine]] itself.
  *
  * Components are serialized and deserialized by default, unless specified
  * otherwise. Attributes on the other hand, are the opposite.
  *
  * Marker [[Component]]s ([[Component]]s without any data) are used for
  * signaling or identification purposes. You can create them by not providing
  * another class as a type parameter and give [[Nothing]] instead.
  *
  * ==Example==
  *
  * {{{
  *  package mypkg.datatype
  *
  *  import basalt.core.datatype.Component
  *
  *  case class Slider(min: Int, max: Int) extends Component
  * }}}
  */
trait Component

// /** Information required by the ongoing ticking and querying processes,
//   * available at runtime execution, dynamically created by an
//   * [[basalt.core.engine.Engine]].
//   *
//   * @tparam I
//   *   The type of the component being described.
//   */
// case class ComponentDescriptor(
//     id: Int,
//     archetypes: Map[ /* K: ID, V: Column */ Int, Int]
// )
