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
package basalt.core.storage.archetype

import basalt.core.engine.Engine
import basalt.core.datatype.Component
import basalt.core.query.{
  OnlyComponents,
  QueryingFilterIterable,
  QueryingFilterIterableTag,
  QueryingFilterTag
}
import cats.collections.BitSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/** A set of components common between a group of entities. Used for sparse
  * component storage to quickly allow faster querying or updating.
  *
  * @tparam F
  *   the effect type used for the engine.
  * @tparam I
  *   the component set for this archetype, must be known at compile-time.
  */
class ComponentArchetype[F[_], I <: OnlyComponents[
  QueryingFilterIterable
]: QueryingFilterIterableTag](using engine: Engine[F])(using
    iterTag: QueryingFilterIterableTag[I]
) {

  /** A [[ConcurrentMap]] holding the components of all entities within this
    * archetype. First dimension: Column, represented by the position of the
    * component ID (int). Second dimension: Row, represented by the position of
    * the entity ID (long).
    */
  private val buffer: Map[Int, Map[Long, Component]] = Map()

  def getComponent[C <: Component: QueryingFilterTag](
      entityId: Long
  ): Option[C] =
    engine.components
      .getId[C]
      .flatMap(componentId => buffer.get(componentId))
      .flatMap(dimension => dimension.get(entityId))
      .map(_.asInstanceOf[C])
}
