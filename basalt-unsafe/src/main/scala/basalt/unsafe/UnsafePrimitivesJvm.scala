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

import sun.misc.Unsafe

given jvmByteMemoryFactory(using unsafe: Unsafe): PrimitiveMemoryFactory[Byte]
with
  def write(address: Pointer, data: Byte): Unit =
    unsafe.putByte(address.toLong, data)
  def read(address: Pointer): Byte = unsafe.getByte(address.toLong)

given jvmIntMemoryFactory(using unsafe: Unsafe): PrimitiveMemoryFactory[Int]
with
  def write(address: Pointer, data: Int): Unit =
    unsafe.putInt(address.toLong, data)
  def read(address: Pointer): Int = unsafe.getInt(address.toLong)

given jvmLongMemoryFactory(using unsafe: Unsafe): PrimitiveMemoryFactory[Long]
with
  def write(address: Pointer, data: Long): Unit =
    unsafe.putLong(address.toLong, data)
  def read(address: Pointer): Long = unsafe.getLong(address.toLong)

given jvmCharMemoryFactory(using unsafe: Unsafe): PrimitiveMemoryFactory[Char]
with
  def write(address: Pointer, data: Char): Unit =
    unsafe.putChar(address.toLong, data)
  def read(address: Pointer): Char = unsafe.getChar(address.toLong)

given jvmDoubleMemoryFactory(using
    unsafe: Unsafe
): PrimitiveMemoryFactory[Double] with
  def write(address: Pointer, data: Double): Unit =
    unsafe.putDouble(address.toLong, data)
  def read(address: Pointer): Double = unsafe.getDouble(address.toLong)

given jvmFloatMemoryFactory(using unsafe: Unsafe): PrimitiveMemoryFactory[Float]
with
  def write(address: Pointer, data: Float): Unit =
    unsafe.putFloat(address.toLong, data)
  def read(address: Pointer): Float = unsafe.getFloat(address.toLong)

given jvmShortMemoryFactory(using unsafe: Unsafe): PrimitiveMemoryFactory[Short]
with
  def write(address: Pointer, data: Short): Unit =
    unsafe.putShort(address.toLong, data)
  def read(address: Pointer): Short = unsafe.getShort(address.toLong)
