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
package basalt.unsafe

/** A reference to a native memory address pointer using opaque types to have
  * the less overhead possible. This is a wrapper around common pointer
  * operations and is intended to be used in conjunction with [[RawType]].
  */
type Pointer = Pointer.Type

/** A reference to a native memory address pointer using opaque types to have
  * the less overhead possible. This is a wrapper around common pointer
  * operations and is intended to be used in conjunction with [[RawType]].
  */
object Pointer:
  opaque type Type = Long

  /** Creates a pointer from a long value.
    *
    * @param value
    *   the long value.
    * @return
    *   the pointer.
    */
  def apply(value: Long): Type =
    value

  /** Calculates the address of an aligned pointer.
    * @param base
    *   The base address.
    * @param index
    *   The index if it is a multibyte element.
    * @param alignment
    *   The alignment of the structure kind.
    * @return
    *   The aligned address.
    */
  inline def buildAddress(base: Pointer, index: Long, alignment: Int): Pointer =
    Pointer(base.toLong + index * alignment)

  extension (pointer: Type)
    // general-purpose methods
    inline def toLong: Long                    = pointer
    inline def +(offset: Long | Pointer): Type = pointer + offset
    inline def -(offset: Long | Pointer): Type = pointer - offset
    inline def *(offset: Long | Pointer): Type = pointer * offset
    inline def /(offset: Long | Pointer): Type = pointer / offset
