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
package basalt.core.collection

type GenerationalKey = GenerationalKey.Type

/** An entry in a generational index vector implementation.
  *
  * ==Background==
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

/** An entry in a generational index vector implementation.
  *
  * @tparam T
  *   the type of the value stored in the entry
  */
enum GenerationalEntry:
  case Occupied[T](key: GenerationalKey, value: T)
  case Free(nextFree: Int, generation: Int)

/** A vector implementation that uses generational indices to uniquely identify
  * entries.
  */
class GenerationalVector[T](
    val data: Vector[GenerationalEntry] = Vector.empty,
    val length: Int = 0,
    val freeListHead: Int = 0
):
  def create(value: T): (GenerationalKey, GenerationalVector[T]) =
    val previous = data(freeListHead)
    if previous == null then
      return (
        GenerationalKey(data.length, 0),
        GenerationalVector(data, length + 1, freeListHead + 1)
      )
    end if
    previous match
      case GenerationalEntry.Free(next, generation) =>
        (
          GenerationalKey(freeListHead, generation),
          GenerationalVector(
            data.updated(
              freeListHead,
              GenerationalEntry.Occupied(
                GenerationalKey(freeListHead, generation),
                value
              )
            ),
            length + 1,
            next
          )
        )
      case _ => throw new IllegalStateException("Corrupt generational vector.")

  def get(key: GenerationalKey): Option[GenerationalEntry.Occupied[T]] =
    val entry = data(key.index)
    if entry == null then return None
    entry match
      case GenerationalEntry.Occupied(key2, value)
          if key2.generation == key.generation =>
        Some(GenerationalEntry.Occupied(key2, value.asInstanceOf[T]))
      case _ => None

  def remove(key: GenerationalKey): GenerationalVector[T] =
    val entry = data(key.index)
    if entry == null then return this
    entry match
      case GenerationalEntry.Occupied(key2, _)
          if key2.generation == key.generation =>
        GenerationalVector(
          data.updated(
            key.index,
            GenerationalEntry.Free(freeListHead, key2.generation + 1)
          ),
          length - 1,
          key.index
        )
      case _ => this

  def set(key: GenerationalKey, value: T): GenerationalVector[T] =
    val entry = data(key.index)
    if entry == null then return this
    entry match
      case GenerationalEntry.Occupied(key2, _)
          if key2.generation == key.generation =>
        GenerationalVector(
          data.updated(
            key.index,
            GenerationalEntry.Occupied(key2, value)
          )
        )
      case _ => this
