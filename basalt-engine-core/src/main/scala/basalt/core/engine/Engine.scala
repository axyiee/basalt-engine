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
package basalt.core.engine

import basalt.core.datatype.{Component, ComponentId, EntityId, EntityRef}
import basalt.core.query.{
  QueryingFilterIterable,
  QueryingFilterIterableTag,
  QueryingFilterTag
}
import basalt.core.query.ComponentFilterTag
import basalt.core.archetype.ArchetypeId

import fs2.Stream

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
trait Engine[F[_]]:
  /** The [[ComponentView]] of this engine. */
  def components: ComponentView[F]

  /** The [[EntityView]] of this engine. */
  def entities: EntityView[F]

  /** Starts the lifecycle of this engine, initializing all resources and
    * components. This starts an infinite loop that will only stop when the
    * engine the effect it is running on is terminated.
    */
  def init: Stream[F, Unit]

trait ComponentView[F[_]]:
  /** Finds the identification of a component of a given type.
    *
    * @tparam C
    *   the type of the component.
    * @return
    *   the identification of the component, if found.
    */
  def getId[C <: Component: ComponentFilterTag]: F[ComponentId]

  /** Extracts a component from an entity. Raises an error if the component is
    * not found.
    *
    * @tparam C
    *   the type of the component.
    * @param entityId
    *   the identification of the entity.
    * @return
    *   the component, if found.
    */
  def extract[C <: Component: ComponentFilterTag](
      entityId: EntityId
  ): F[C]

  /** Extracts all components from an entity. Raises an error if the entity is
    * not found.
    *
    * @param entityId
    *   the identification of the entity.
    * @return
    *   a stream of all components of the entity.
    */
  def extractAll(
      entityId: EntityId
  ): Stream[F, Component]

  /** Removes a component from an entity.
    *
    * @tparam C
    *   the type of the component.
    * @param entityId
    *   the identification of the entity.
    */
  def remove[C <: Component: ComponentFilterTag](
      entityId: EntityId
  ): F[Unit]

  /** Inserts or updates a component of a given type on an entity. Raises an
    * error if the entity is not found.
    *
    * @tparam C
    *   the type of the component.
    * @param entityId
    *   the identification of the entity.
    * @param content
    *   the component to be inserted or updated.
    */
  def set[C <: Component: ComponentFilterTag](
      entityId: EntityId,
      content: C
  ): F[Unit]

  // /** Updates or inserts a set of components within the same type into a set of
  //   * entities. Raise an error if any of the entities is not found.
  //   *
  //   * @tparam C
  //   *   the type of the component.
  //   * @param entityId
  //   */
  // def setBulk[C <: Component: ComponentFilterTag](
  //     content: (EntityId, C)*
  // ): F[Unit]

  // /** Removes a set of components within the same type from a set of entities.
  //   * Raise an error if any of the entities is not found.
  //   *
  //   * @tparam C
  //   *   the type of the component.
  //   * @param entityId
  //   */
  // def removeBulk[C <: Component: ComponentFilterTag](
  //     entityId: EntityId*
  // ): F[Unit]

trait EntityView[F[_]]:
  /** Creates a new entity.
    *
    * @tparam C
    *   the type of the component.
    * @return
    *   a reference for accessing the entity attributes and components.
    */
  def create: F[EntityRef[F]]

  /** Deletes an entity from the engine, removing all of its components and
    * attributes.
    *
    * @param entityId
    *   the identification of the entity.
    */
  def delete(entityId: EntityId): F[Unit]

  /** Creates a reference for accessing and modifying the components and
    * attributes of an entity. Raises an error if not found.
    *
    * @tparam A
    *   the type of the archetype.
    * @param entityId
    *   the identification of the entity.
    * @return
    *   a reference for accessing the entity attributes and components.
    */
  def get(entityId: EntityId): Either[NoSuchElementException, EntityRef[F]]

  /** Finds the archetype ID of an entity. Raises an error if not found.
    *
    * @param entityId
    *   the identification of the entity.
    * @return
    *   the identification of the archetype.
    */
  def getArchetypeId(
      entityId: EntityId
  ): Either[NoSuchElementException, ArchetypeId]
