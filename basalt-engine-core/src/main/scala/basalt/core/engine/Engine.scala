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
package basalt.core.engine

import basalt.core.datatype.{Component, Entity}
import basalt.core.query.{
  QueryingFilterIterable,
  QueryingFilterIterableTag,
  QueryingFilterTag
}

/** An overview and supervisor of all entities, components, attributes,
  * resources, and their associated metadata.
  *
  * Each [Entity] has an unique identification and a set of components and
  * attributes. Each component must be associated with a type used up to a
  * single time on each [Entity]. Entities and components can be either created,
  * removed, updated, or queried using a given [Engine] implementation which
  * supervises the environment of a specific platform implementation.
  *
  * ==Attributes==
  *
  * [Attribute]s are a derivation of components often used to save resources or
  * store data temporarily within the RAM. Those aren't serialized unless
  * manually specified otherwise. Attributes are by default, data already
  * provided by a platform implementation - such as hunger, health, display
  * name, etc. - therefore not worth the data persistence.
  *
  * ==Resources==
  *
  * [Resource]s are unique instances of a data type stored independent from
  * entities. Those are available in all contexts, to every entity, and are used
  * for storing global context or data that is not tied to any specific entity,
  * such as configurations or game states.
  *
  * @tparam F
  *   the effect type used for the engine.
  */
trait Engine[F[_]] {
  def query[I <: QueryingFilterIterable: QueryingFilterIterableTag](using
      QueryingFilterIterableTag[I]
  ): fs2.Stream[F, (Entity[F], I)]

  def components: ComponentView[F]

  def entities: EntityView[F]
}

trait ComponentView[F[_]] {
  def getId[C <: Component: QueryingFilterTag](using
      QueryingFilterTag[C]
  ): Option[Int]

  def extract[C <: Component: QueryingFilterTag](
      targetEntityId: Long
  ): Either[IllegalArgumentException, Option[C]]

  def remove[C <: Component: QueryingFilterTag](
      targetEntityId: Long
  ): F[Option[C]]

  def update[C <: Component: QueryingFilterTag](
      targetEntityId: Long,
      content: C
  ): F[Unit]
}

trait EntityView[F[_]] {
  def create: F[Entity[F]]

  def delete(id: Long): F[Unit]
}
