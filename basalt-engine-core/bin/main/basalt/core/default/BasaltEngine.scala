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
package basalt.core.default

import basalt.core.descriptor.{
  ArchetypesDescriptor,
  ComponentsDescriptor,
  EntitiesDescriptor
}
import basalt.core.engine.Engine
import basalt.core.event.internal.InternalEvent
import basalt.core.query.{QueryingFilterIterable, QueryingFilterIterableTag}

import cats.effect.kernel.Async
import cats.syntax.all._

import fs2.Stream
import fs2.concurrent.Topic
import cats.effect.kernel.Ref

/** Default, general-purpose implementation of the Basalt [[Engine]] API.
  *
  * @param tps
  *   How many ticks are going to be performed within a second.
  * @tparam F
  *   the effect type used for the engine.
  */
class BasaltEngine[F[_]: Async](
    val tps: Int = 20,
    override val components: BasaltComponentView[F],
    override val entities: BasaltEntityView[F],
    val events: Ref[F, List[InternalEvent]]
) extends Engine[F]:
  val pipeline = EnginePipeline[F](this, events, List.empty, tps)
  override def init: Stream[F, Unit] =
    pipeline.loop

object BasaltEngine:
  def apply[F[_]: Async](tps: Int = 20): F[BasaltEngine[F]] =
    for
      archetypes <- ArchetypesDescriptor[F]
      components <- ComponentsDescriptor[F]
      entities = new EntitiesDescriptor[F]
      events <- Ref[F].of[List[InternalEvent]](List.empty)
      view1 = BasaltComponentView[F](components, entities, archetypes, events)
      view2 = BasaltEntityView[F](entities, archetypes, view1, events)
    yield new BasaltEngine[F](tps, view1, view2, events)
