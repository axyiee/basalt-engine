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
package basalt.core.default

import basalt.core.engine.Engine
import cats.effect.kernel.{Async, Temporal}
import concurrent.duration.DurationInt
import cats.syntax.all.*
import scala.concurrent.duration.FiniteDuration
import cats.effect.kernel.Ref

private case class TickInfo(ticksPerSecond: Int, tickDuration: FiniteDuration)

/** Processes entities, components, queries, and systems given their previous
  * and current state.
  *
  * @tparam F
  *   the effect type used for the engine.
  */
class EnginePipeline[F[_]: Async](
    private val engine: BasaltEngine[F],
    private val tps: Int = 20
) {

  /** Generates information about ticking based on the tick period ([[tps]]).
    * @param tps
    *   How many ticks are going to be performed within a second.
    */
  def getTickInfo(tps: Int): TickInfo =
    TickInfo(tps, 1.second / tps)

  def intercept(tickDelta: Int): F[Unit] =
    Async[F].unit

  def loop: F[Unit] =
    val ticking     = getTickInfo(tps)
    val currentTick = Ref[F].of(1)
    currentTick.flatMap(ref =>
      ref.get
        .flatTap(value => intercept(value))
        .flatMap(_ =>
          ref.update(curr =>
            if curr < ticking.ticksPerSecond then curr + 1 else 1
          )
        )
        .flatMap(_ => Temporal[F].sleep(ticking.tickDuration))
        .foreverM
    )
}
