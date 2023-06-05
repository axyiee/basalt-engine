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

/** A type regarding all possible primitives for an unsafe operation.
  */
type Primitive = Byte | Int | Long | Char | Double | Float | Short

/** A factory helper responsible for managing the allocation and freeing of new
  * pointers.
  */
trait UnsafePointerFactory:
  /** Computes a new memory allocation under the given [size].
    *
    * @param size
    *   The size of the allocation.
    * @return
    *   The address of the result allocation.
    */
  def alloc(size: Long): Pointer

  /** Reallocates a value at a specific memory [[address]] with the given
    * [[data]].
    *
    * @param address
    *   The memory address to be reallocated.
    * @param data
    *   The data to be reallocated at the given memory [[address]].
    */
  def realloc[A <: Primitive](address: Pointer, data: A)(using
      PrimitiveMemoryFactory[A]
  ): Unit

  /** Reallocates the [[length]] of a structure at a specific memory
    * [[address]].
    * @param address
    *   The memory address to be reallocated.
    * @param length
    *   The length of the structure to be reallocated.
    */
  def reallocBlock(address: Pointer, length: Long): Unit

  /** Computes the freeing of an existing memory allocation under the given
    * [address].
    *
    * @param address
    *   The address of the allocation.
    */
  def freePointer(address: Pointer): Unit

  /** Access the [[Primitive]] value with the given [[offset]] of the given
    * [[address]].
    *
    * @param address
    *   The address to be accessed.
    * @return
    *   A representation of a [[Primitive]] value at the given [[address]].
    */
  def visit[A <: Primitive](address: Pointer)(using
      PrimitiveMemoryFactory[A]
  ): A

  extension (@annotation.unused pointer: Pointer.type)
    /** Allocates a structure into the native memory (heap). This returns the
      * address of the start of the allocated memory. This is really useful when
      * working with large structure like arrays.
      *
      * @param size
      *   The size of the structure to allocate to the memory.
      * @return
      *   The address of the result allocation.
      */
    def allocate(size: Int): Pointer = this.alloc(size)

  extension (pointer: Pointer)

    /** Frees the memory taken by the memory referenced by this pointer.
      */
    def free(): Unit =
      this.freePointer(pointer)

    /** Access the [[Byte]] value with the given [[offset]] of this address.
      */
    def access[A <: Primitive](using PrimitiveMemoryFactory[A]): A =
      this.visit(pointer)

    /** Reallocates a value at this memory address with the given [[data]].
      *
      * @param data
      *   The data to be reallocated at the given memory [[address]].
      * @return
      *   The computation of a memory reallocation.
      */
    def store[A <: Primitive](data: A)(using PrimitiveMemoryFactory[A]): Unit =
      this.realloc(pointer, data)

/** Acts into writing or reading values of/into the memory table for a specific
  * primitive kind.
  *
  * @tparam A
  *   The primitive kind to be managed.
  */
trait PrimitiveMemoryFactory[A <: Primitive]:
  def write(address: Pointer, data: A): Unit
  def read(address: Pointer): A
