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
package basalt.core.descriptor

import basalt.core.archetype.{ArchetypeId, ComponentArchetype}
import basalt.core.datatype.{Component, ComponentId, EntityId}
import basalt.core.query.{ComponentFilterTag, QueryingFilter, QueryingFilterTag}

import cats.effect.kernel.{Async, Sync}
import cats.effect.std.AtomicCell
import cats.syntax.all._

import collection.mutable.{Map, HashMap}
import scala.collection.mutable.LongMap

/** Information required by the ongoing ticking and querying processes,
  * available at runtime execution, dynamically created by an
  * [[basalt.core.engine.Engine]].
  */
class ComponentsDescriptor[F[_]: Sync] private (
    val counter: AtomicCell[F, ComponentId],
    val indices: Map[ComponentFilterTag[QueryingFilter], ComponentId]
):
  def getId[C <: Component: QueryingFilterTag]: F[Long] =
    for
      index <- Sync[F].pure(
        indices.get(
          summon[QueryingFilterTag[C]]
            .asInstanceOf[ComponentFilterTag[QueryingFilter]]
        )
      )
      id <- index match
        case Some(id) => Sync[F].pure(id)
        case None     => init[C]
    yield id

  def init[C <: Component: QueryingFilterTag]: F[Long] =
    counter
      .getAndUpdate(_ + 1)
      .flatTap(id =>
        Sync[F].delay(
          indices.put(
            summon[QueryingFilterTag[C]]
              .asInstanceOf[ComponentFilterTag[QueryingFilter]],
            id
          )
        )
      )

object ComponentsDescriptor:
  def apply[F[_]: Async]: F[ComponentsDescriptor[F]] =
    for
      counter <- AtomicCell[F].of[ComponentId](1L)
      indices <- Sync[F].pure(
        HashMap[ComponentFilterTag[QueryingFilter], Long]()
      )
    yield new ComponentsDescriptor[F](counter, indices)
