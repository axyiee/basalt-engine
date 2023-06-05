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

import concurrent.duration.{DurationInt, FiniteDuration}

/** Relevant information to the ticking process. */
case class TickInfo(ticksPerSecond: Int, tickDuration: FiniteDuration):

  /** The number of tick increments until the tick validation threshold is
    * reached.
    */
  val tickValidationThreshold: Tick =
    Tick(ticksPerSecond * 60 * 60)

    /** The maximum age of a change that can be detected.
      */
  val maxChangeAge =
    Int.MaxValue - (2 * tickValidationThreshold.toInt - 1)

object TickInfo:

  /** Constructs a [[TickInfo]] type from the ticks per second information.
    * @param ticksPerSecond
    *   the ticks per second.
    * @return
    *   the tick info.
    */
  def apply(ticksPerSecond: Int): TickInfo =
    TickInfo(ticksPerSecond, 1.second / ticksPerSecond)

type Tick = Tick.Type

/** A value that tracks the time elapsed relative to other systems. This is
  * often used to detect changes within the environment.
  */
object Tick:
  opaque type Type = Int

  /** Constructs a tick from an integer value.
    * @param tick
    *   the tick value.
    * @return
    *   the tick.
    */
  def apply(tick: Int): Tick =
    tick

  extension (tick: Tick)
    def +(other: Tick | Int): Tick     = tick.toInt + other
    def -(other: Tick | Int): Tick     = tick.toInt - other
    def *(other: Tick | Int): Tick     = tick.toInt * other
    def /(other: Tick | Int): Tick     = tick.toInt / other
    def %(other: Tick | Int): Tick     = tick.toInt % other
    def <(other: Tick | Int): Boolean  = tick.toInt < other
    def <=(other: Tick | Int): Boolean = tick.toInt <= other
    def >(other: Tick | Int): Boolean  = tick.toInt > other
    def toInt: Int                     = tick

    /** Returns the difference between two ticks.
      * @param other
      *   the other tick.
      * @return
      *   the difference between the two ticks.
      */
    def relativeTo(other: Tick): Tick =
      (tick - other) % Math.pow(2, Integer.SIZE).toInt

    /** Whether this [[Tick]] occurred since's the system's last run timestamp.
      */
    def isNewerThan(sysLastRun: Tick, sysCurrRun: Tick): Boolean =
      (sysCurrRun - sysLastRun) > (sysCurrRun - tick)

    /** Validates whether the tick is within the tick validation threshold.
      * @param tickInfo
      *   the tick information.
      * @param sysThisRun
      *   the system's current run timestamp.
      * @return
      *   this [[tick]] within the tick validation threshold.
      */
    def validate(sysThisRun: Tick, tickInfo: TickInfo): Tick =
      if sysThisRun.relativeTo(tick) > tickInfo.tickValidationThreshold then
        sysThisRun.relativeTo(tickInfo.tickValidationThreshold)
      else tick
