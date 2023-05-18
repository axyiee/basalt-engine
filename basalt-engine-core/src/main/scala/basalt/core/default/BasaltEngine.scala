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

import basalt.core.datatype.Entity
import basalt.core.engine.Engine
import basalt.core.query.{
  OnlyComponents,
  QueryingFilterIterable,
  QueryingFilterIterableTag
}
import basalt.core.storage.archetype.ComponentArchetype

import java.util.concurrent.ConcurrentMap
import scala.collection.mutable.{ArrayBuffer, Map}
import cats.syntax.all._
import cats.effect.kernel.Async

type ArchetypeCombIndex =
  Map[ /* K: Component combination, V: Archetype ID */ OnlyComponents[
      QueryingFilterIterable
    ], Int]
type ArchetypeIndex[F[_]] =
  Map[Int, /* K: ID, V: Archetype data */ ComponentArchetype[F, _]]
type ComponentIndex = Map[Int, /* K: ID, V: Column */ Map[Int, Int]]
type EntityIndex    = Map[ /* K: ID, V: Archetype Index */ Long, Int]
type ComponentIdIndex =
  Map[ /* K: Component Class Qualified Name, V: ID */ String, Int]

/** Default, general-purpose implementation of the Basalt [[Engine]] API.
  *
  * @param tps
  *   How many ticks are going to be performed within a second.
  * @tparam F
  *   the effect type used for the engine.
  */
class BasaltEngine[F[_]: Async](val tps: Int = 20) extends Engine[F] {
  val archetypeCombIndex: ArchetypeCombIndex = Map()
  val archetypeIndex: ArchetypeIndex[F]      = Map()
  val entityIndex: EntityIndex               = Map()
  val componentDescriptors: ComponentIndex   = Map()
  val attributeDescriptors: ComponentIndex   = Map()
  val componentIds: ComponentIdIndex         = Map()

  override val components = new BasaltComponentView[F](this)
  override val entities   = new BasaltEntityView[F](this)
  private val pipeline    = EnginePipeline[F](this, tps)

  def query[I <: QueryingFilterIterable: QueryingFilterIterableTag](using
      QueryingFilterIterableTag[I]
  ): fs2.Stream[F, (Entity[F], I)] = fs2.Stream.empty
}
