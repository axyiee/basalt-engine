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
package basalt.unsafe

/** A low-level and unsafe type class regarding explicit native (heap) memory
  * management within the JVM. It is also useful to use [[sun.misc.Unsafe]] as a
  * behind-the-scenes implementation whenever possible to ensure maximum
  * performance and efficiency when working with native memory.
  *
  * @tparam A
  *   The kind of structure to be dynamically managed.
  */
trait UnsafeStructKind[A]:
  /** The memory alignment for this type. An alignment is the number of bytes a
    * value that a memory pointer points to must be divisible by in order to be
    * valid for this type.
    */
  def alignment: Int

  /** Allocates a value of type [A] to the memory and returns its result
    * address.
    *
    * @param data
    *   The data to allocate to the heap.
    *
    * @return
    *   The address of the allocated data.
    */
  def alloc(data: A): Pointer

  /** Allocates an empty structure within the given [length] directly into the
    * memory table.
    *
    * @param length
    *   The length of the structure to be allocated.
    *
    * @return
    *   The start address of the result memory block.
    */
  def allocEmpty(length: Long): Pointer

  /** Reallocates a value of type [A] at the given [address]. This will modify
    * the value at the address given by the formula of `address + index *
    * alignment`.
    *
    * @param address
    *   The address to reallocate at.
    * @param index
    *   The index to reallocate at. It can of course be set to zero if it isn't
    *   necessary.
    * @param data
    *   The new data to be allocated.
    * @return
    *   The address of the reallocated data.
    */
  def store(address: Pointer, index: Long, data: A): Pointer

  /** Reallocates and change the [[length]] of a structure at the given
    * [[address]]. The structure will have a length given by the formula of
    * `length * alignment`.
    *
    * @param address
    *   The address to reallocate at.
    * @param length
    *   The new length of the reallocated structure.
    */
  def realloc(address: Pointer, length: Long): Unit

  /** Deserialize the given [[address]] data back into data of type [A].
    */
  def read(address: Pointer): A

  /** Return whether the given pointer is valid for this type.
    *
    * @param pointer
    *   The pointer to check.
    * @return
    *   Whether the pointer is valid for this type.
    */
  def is(pointer: Pointer): Boolean =
    pointer.toLong % alignment == 0

  extension (pointer: Pointer)
    /** Deserialize this [[Pointer]] data back into data of type [A].
      */
    def readAs: A = this.read(pointer)
