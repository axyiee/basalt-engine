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
package basalt.core.datatype

import basalt.core.engine.Engine
import cats.collections.BitSet
import basalt.core.query.QueryingFilter

type ComponentId = Long

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
  * signaling or identification purposes.
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
trait Component extends QueryingFilter

type ComponentSet = ComponentSet.Type

/** An data type regarding an ordered set of [Component]s based on bitsets. Each
  * component ID is mapped to a bit in the bitset, and the bitset is used to
  * store the components in a ordered way to avoid archetypes with the same
  * components but in different orders.
  */
object ComponentSet {
  opaque type Type = BitSet

  /** Creates a new [[ComponentSet]] with the given component IDs. */
  def of(ids: ComponentId*): ComponentSet =
    BitSet(ids.map(_.toInt): _*)

  extension (set: ComponentSet)
    def toBitSet: BitSet                     = set
    def toSet: Set[Int]                      = set.toBitSet.toSet
    def apply(target: ComponentId): Boolean  = set.toBitSet(target.toInt)
    def +(target: ComponentId): ComponentSet = set.toBitSet + target.toInt
    def -(target: ComponentId): ComponentSet = set.toBitSet - target.toInt
    def iterator: Iterator[Int]              = set.toBitSet.iterator
}
