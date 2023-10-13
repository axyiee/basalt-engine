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
package basalt.core.datatype

import basalt.core.archetype.ArchetypeId
import basalt.core.collection.GenerationalKey
import basalt.core.descriptor.{
  ArchetypesDescriptor,
  ComponentsDescriptor,
  EntitiesDescriptor
}
import basalt.core.engine.{Engine, ComponentView, EntityView}
import basalt.core.query.{QueryingFilter, QueryingFilterTag}

import cats.syntax.all._
import cats.effect.kernel.Sync

type EntityId   = GenerationalKey
type EntityMeta = EntityMeta.Type

/** An entity is an identifier for a set of [[Component]]s on a specific
  * [[Engine]] context.
  *
  * Each entity has an unique identification and a set of [[Component]]s and
  * [[Attribute]]s. Each [[Component]] must be associated with a type used up to
  * a single time on each entity.
  *
  * This is a reference to the entity location within an archetype.
  */
object EntityMeta:
  opaque type Type = Long

  /** Constructs a new generational index.
    * @param generation
    *   the generation of the entry
    * @param index
    *   the identifier of the entry
    */
  def apply(archetypeId: Long, archetypeRow: Int): EntityMeta =
    (archetypeId << 32) | (archetypeRow & 0xffffffff)

  extension (key: EntityMeta)
    def toBits: Long      = key
    def archetypeId: Int  = (toBits >> 32).toInt
    def archetypeRow: Int = (toBits & 0xffffffff).toInt

/** An entity is an identifier for a set of [[Component]]s on a specific
  * [[Engine]] context.
  *
  * Each entity has an unique identification and a set of [[Component]]s and
  * [[Attribute]]s. Each [[Component]] must be associated with a type used up to
  * a single time on each entity.
  *
  * This is a reference for modifying an Entity within an [[Engine]] context.
  *
  * @see
  *   basalt.core.collection.GenerationalEntry
  */
class EntityRef[F[_]: Sync](
    val id: EntityId,
    private val entities: EntityView[F],
    private val _components: ComponentView[F]
):
  /** Finds the archetype ID of the entity.
    *
    * @return
    *   the archetype ID of the entity.
    */
  def archetypeId: Either[NoSuchElementException, ArchetypeId] =
    entities.getArchetypeId(id)

  /** Whether this entity is still alive.
    *
    * @return
    *   true if the entity is alive, false otherwise.
    */
  def isAlive: Boolean = archetypeId.isRight

  /** Returns the set of components this entity has. Returns an empty iterable
    * if the entity is not alive.
    *
    * @return
    *   the set of components this entity has.
    */
  def components: fs2.Stream[F, Component] =
    _components.extractAll(id)

  /** Adds or updates a component to the entity referenced by this.
    *
    * @param component
    *   the component to be added.
    * @tparam C
    *   the type of the component.
    * @return
    *   this entity reference.
    */
  def set[C <: Component](component: C)(using
      tag: QueryingFilterTag[C]
  ): F[EntityRef[F]] =
    _components.set(id, component).map(_ => this)

  /** Removes a component from the entity referenced by this.
    *
    * @param component
    *   the component to be removed.
    * @tparam C
    *   the type of the component.
    * @return
    *   this entity reference.
    */
  def remove[C <: Component](using
      tag: QueryingFilterTag[C]
  ): F[EntityRef[F]] =
    _components.remove(id).map(_ => this)

  /** Retrieves a component from the entity referenced by this. Returns None if
    * either the entity is not alive or the entity does not have a component
    * within the given type.
    *
    * @param component
    *   the component to be retrieved.
    * @tparam C
    *   the type of the component.
    * @return
    *   the component if it exists, None otherwise.
    */
  def get[C <: Component](using tag: QueryingFilterTag[C]): F[C] =
    _components.extract(id)
