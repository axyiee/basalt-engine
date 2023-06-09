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
package basalt.core.collection

import scala.collection.mutable.ArrayBuffer

type GenerationalKey = GenerationalKey.Type

/** An entry in a generational index vector implementation.
  *
  * ==Background==
  *
  * An generational indices-based data structure used for uniquely identifying
  * entities. Inlined/opaque type aliased to an [[Int]]eger as a
  * bit-packed/encoded data-structure divided into two parts:
  *   - the first 32 bits are used for the index
  *   - the last 32 bits are used for the generation
  *
  * ===Motivation===
  *
  * The motivation for this data structure is to have a way to uniquely identify
  * entities even when recycling their indexes. Each time an entity is deleted,
  * the generation count goes up by one. This way, the index can be recycled
  * without the risk of having invalid references to the previous entity with
  * the same index.
  */
object GenerationalKey:
  opaque type Type = Long

  /** Constructs a new generational index.
    * @param generation
    *   the generation of the entry
    * @param index
    *   the identifier of the entry
    */
  def apply(index: Int, generation: Int = 0): GenerationalKey =
    (generation << 32) | (index & 0xffffffff)

  extension (key: GenerationalKey)
    def toBits: Long    = key
    def generation: Int = (toBits >> 32).toInt
    def index: Int      = (toBits & 0xffffffff).toInt

/** An entry on a generational indices vector implementation.
  *
  * @tparam T
  *   the type of the value stored in the entry
  */
sealed trait GenerationalEntry[T] {
  val generation: Int
}

/** An occupied entry on a generational indices vector implementation.
  *
  * @param key
  *   the key of the entry
  * @tparam T
  *   the type of the value stored in the entry
  */
case class Occupied[T](key: GenerationalKey, value: T)
    extends GenerationalEntry[T]:
  override val generation: Int = key.generation

/** A free entry on a generational indices vector implementation.
  *
  * @param next
  *   the next free entry
  * @tparam T
  *   the type of the value stored in the entry
  */
case class Free(next: Int, override val generation: Int)
    extends GenerationalEntry[Nothing]

/** A vector implementation that uses generational indices to uniquely identify
  * entries.
  */
class GenerationalVector[T](
    val data: ArrayBuffer[GenerationalEntry[T]] = ArrayBuffer.empty,
    private var _length: Int = 0,
    private var freeListHead: Int = 0
):
  def length: Int = _length

  def create(value: T): GenerationalKey =
    val previous = data.lift(freeListHead)
    if previous.isEmpty then
      val key = GenerationalKey(data.length, 0)
      data.append(Occupied(key, value))
      _length += 1
      freeListHead = key.index + 1
      return key
    previous match
      case Some(Free(next, generation)) =>
        val key = GenerationalKey(freeListHead, generation)
        data.update(
          key.index,
          Occupied(key, value)
        )
        freeListHead = next
        _length += 1
        return key
      case _ => throw new IllegalStateException("Corrupt generational vector.")

  def get(key: GenerationalKey): Option[Occupied[T]] =
    data.lift(key.index) match
      case Some(Occupied(key2, value)) if key2.generation == key.generation =>
        Some(Occupied(key2, value.asInstanceOf[T]))
      case _ => None

  def remove(key: GenerationalKey) =
    val entry = data.lift(key.index)
    entry match
      case Some(Occupied(key2, _)) if key2.generation == key.generation =>
        data.update(
          key.index,
          Free(freeListHead, key2.generation + 1).asInstanceOf
        )
        freeListHead = key.index
        _length -= 1
      case _ => ()

  def set(key: GenerationalKey, value: T) =
    val entry = data.lift(key.index)
    entry match
      case Some(Occupied(key2, _)) if key2.generation == key.generation =>
        data.update(
          key.index,
          Occupied(key2, value)
        )
      case _ => this

  def contains(key: GenerationalKey): Boolean =
    val entry = data.lift(key.index)
    entry match
      case Some(Occupied(key2, _)) if key2.generation == key.generation =>
        true
      case _ => false
