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

/** An [[Attribute]] is a special type of built-in [[Component]] given by the
  * platform implementation. [[Attribute]]s are used to store data that is
  * already provided by the platform, such as hunger, health, display name, etc.
  *
  * [[Attribute]]s are not serialized and deserialized by default, unless
  * specified otherwise. Components on the other hand, are the opposite.
  *
  * Marker [[Attribute]]s (attributes without any data) are used for signaling
  * or identification purposes.
  *
  * ==Example==
  *
  * {{{
  *  case object Hunger extends Attribute[Int]
  *
  *  case object Player extends Attribute[Nothing]
  *
  *  case object Health extends Attribute[Int]
  * }}}
  */
trait Attribute extends Component
