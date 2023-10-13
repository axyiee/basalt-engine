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
package basalt.core.archetype

import basalt.core.datatype.{
  Component,
  ComponentId,
  ComponentSet,
  EntityId,
  Tick
}
import basalt.core.engine.ComponentView
import basalt.core.query.{QueryingFilter, QueryingFilterIterable, Fin}
import basalt.core.syntax.all._

import cats.collections.BitSet
import cats.effect.kernel.Sync
import cats.syntax.all._

import collection.mutable.{LongMap, ArrayBuffer}
import cats.effect.kernel.Ref

type ArchetypeId = ComponentId

/** An entry on an archetype.
  * @param data
  *   the component data.
  * @param added
  *   the tick when the component was added.
  * @param changed
  *   the tick when the component was changed.
  */
case class ArchetypeEntry(data: Component, added: Tick, changed: Tick)

/** Adding or removing [[Component]]s moves an entity to a new
  * [[ComponentArchetype]]. An edge is a caching technique used to quickly move
  * entities between archetypes when adding or removing components.
  */
case class ArchetypeEdge[F[_]: Sync](
    val add: ComponentArchetype[F],
    val remove: ComponentArchetype[F]
)

/** A set of components common between a group of entities. Used for sparse
  * component storage to quickly allow faster querying or updating.
  *
  * @tparam F
  *   the effect type used for the engine.
  * @tparam I
  *   the component set for this archetype, must be known at compile-time.
  */
class ComponentArchetype[F[_]: Sync](
    val id: ArchetypeId,
    val components: ComponentSet,
    private val findArchetype: (ComponentSet) => F[ComponentArchetype[F]]
):
  private val edges: LongMap[ArchetypeEdge[F]] = LongMap.empty

  /** A [[LongMap]] holding the position of components of all entities within
    * this archetype. First dimension: Column, represented by the position of
    * the component ID (long). Second dimension: Row, represented by the
    * position of the entity ID (long).
    */
  private val buffer: LongMap[LongMap[Component]] =
    LongMap(
      components.iterator
        .map(component => component.toLong -> LongMap.empty[Component])
        .toSeq: _*
    )

  /** A vector holding the identification of all entities within this archetype.
    */
  private val entities: ArrayBuffer[EntityId] = ArrayBuffer.empty

  /** Returns whether this archetype is empty.
    * @return
    *   true if this archetype is empty, false otherwise.
    */
  def isEmpty: Boolean = buffer.isEmpty

  /** Returns whether this archetype has a component with the given ID.
    * @param componentId
    *   the component ID.
    * @return
    *   true if this archetype has a component with the given ID, false
    *   otherwise.
    */
  def has(componentId: ComponentId): Boolean = buffer.contains(componentId)

  /** Returns an iterable of all entities within this archetype.
    * @return
    *   an iterable of all entities within this archetype.
    */
  def entityIter: Iterable[EntityId] = entities

  /** Returns whether an entity is present in this archetype.
    * @param entityId
    *   the entity ID.
    * @return
    *   true if the entity is present in this archetype, false otherwise.
    */
  def hasEntity(entityId: EntityId): Boolean = entities.contains(entityId)

  /** Retrieves a [[Component]] from an entity given directly the
    * [[ComponentId]].
    * @param componentId
    *   the component ID.
    * @param entityId
    *   the entity ID.
    * @return
    *   the component, if present.
    */
  def getComponent(
      entityId: EntityId,
      componentId: ComponentId
  ): Option[Component] =
    buffer.get(componentId).flatMap(dimension => dimension.get(entityId.toBits))

  /** Retrieves a [[Component]] from an entity given directly the
    * [[ComponentId]] as an [[F]] effect of [[Component]]. Raises
    * [[NoSuchElementException]] if the component is not present.
    * @param componentId
    *   the component ID.
    * @param entityId
    *   the entity ID.
    * @return
    *   an effect of the component, if not present raises an error.
    */
  def liftComponent[C <: Component](
      entityId: EntityId,
      componentId: ComponentId
  ): F[C] =
    Sync[F]
      .fromOption(
        getComponent(entityId, componentId),
        new NoSuchElementException(
          s"Component $componentId is not present in entity $entityId."
        )
      )
      .map(_.asInstanceOf[C])

  /** Retrieves all [[Component]]s from an entity.
    * @param entityId
    *   the entity ID.
    * @return
    *   the components, if present.
    */
  def getComponents(entityId: EntityId): Iterable[Component] =
    buffer.values.flatMap(dimension => dimension.get(entityId.toBits))

  /** Retrieves all [[Components]] from an entity matching a component set.
    * @param entityId
    *   the entity ID.
    * @param components
    *   the set of components ids.
    * @return
    *   the components, if present.
    */
  def getComponents(
      entityId: EntityId,
      components: ComponentSet
  ): QueryingFilterIterable =
    var iter: QueryingFilterIterable = Fin
    components.iterator.foreach { componentId =>
      getComponent(entityId, componentId).foreach(res => iter = res |: iter)
    }
    iter

  /** Removes an entity from this archetype.
    * @param entityId
    *   the entity ID.
    * @return
    *   an effect of the operation, [F] of [[Unit]] if successful.
    */
  def removeEntity(entityId: EntityId): F[Unit] =
    Sync[F].delay {
      buffer.foreach { case (_, dimension) =>
        dimension.remove(entityId.toBits)
      }
    }

  /** Moves an entity from another archetype to this archetype.
    * @param from
    *   the archetype to move the entity from.
    * @param entity
    *   the entity ID.
    * @return
    *   an effect of the operation, [F] of [[Unit]] if successful.
    */
  def moveEntity(from: ComponentArchetype[F], entity: EntityId): F[Unit] =
    Sync[F].delay {
      from.buffer.foreach { (fromId, fromDimension) =>
        val toDimension = this.buffer.get(fromId)
        if toDimension.isEmpty then
          throw new IllegalStateException(
            s"Invalid archetype move operation; missing components."
          )
        val component = fromDimension.remove(entity.toBits)
        if component.isEmpty then
          throw new IllegalStateException(
            s"Entity $entity is corrupted; component $fromId is missing."
          )
        component.foreach(it => toDimension.get.put(entity.toBits, it))
      }
      from.entities.remove(from.entities.indexOf(entity))
      if !entities.contains(entity) then entities.addOne(entity)
    }

  // /** Moves a set of entities from another archetype to this archetype.
  //   * @param from
  //   *   the archetype to move the entities from.
  //   * @param entities
  //   *   the entity IDs.
  //   * @return
  //   *   an effect of the operation, [F] of [[Unit]] if successful.
  //   */
  // def moveEntityBulk(
  //     from: ComponentArchetype[F],
  //     entities: EntityId*
  // ): F[Unit] =
  //   Sync[F].delay {
  //     from.buffer.foreach { (fromId, fromDimension) =>
  //       val toDimension = this.buffer.get(fromId)
  //       entities.foreach { entity =>
  //         val component = fromDimension.remove(entity.toBits)
  //         if !component.isEmpty then
  //           throw new IllegalStateException(
  //             s"Entity $entity is corrupted; component $fromId is missing."
  //           )
  //         component.foreach(it => toDimension.put(entity.toBits, it))
  //       }
  //     }
  //   }

  /** Inserts a [[Component]] into an entity. The entity is going to move
    * archetypes if the component is not present in this archetype.
    * @param entityId
    *   the entity ID.
    * @param componentId
    *   the component ID.
    * @param component
    *   the component data.
    * @return
    *   an effect of the operation, [F] of [[ComponentArchetype]] if successful.
    */
  def addComponent(
      entityId: EntityId,
      componentId: ComponentId,
      component: Component
  ): F[ComponentArchetype[F]] =
    buffer.get(componentId) match
      case Some(dimension) =>
        Sync[F].pure(this).flatTap { _ =>
          Sync[F].delay {
            dimension.put(entityId.toBits, component)
            if !entities.contains(entityId) then entities.addOne(entityId)
          }
        }
      case None =>
        edges.get(componentId) match
          case Some(edge) =>
            Sync[F].pure(edge.add).flatTap { add =>
              add
                .addComponent(entityId, componentId, component)
                .flatTap(_ => add.moveEntity(this, entityId))
            }
          case None =>
            for
              existingEdge <- Sync[F].pure(edges.get(componentId))
              archetype <- existingEdge match
                case Some(edge) =>
                  Sync[F].pure(edge.add).flatTap { add =>
                    add
                      .addComponent(entityId, componentId, component)
                      .flatTap(_ => add.moveEntity(this, entityId))
                  }
                case None =>
                  findArchetype(components + componentId)
                    .flatTap(arch =>
                      Sync[F]
                        .pure(ArchetypeEdge[F](add = arch, remove = this))
                        .flatTap(
                          _.add.addComponent(entityId, componentId, component)
                        )
                        .flatTap(_.add.moveEntity(this, entityId))
                        .flatTap(edge =>
                          Sync[F].delay(edges.put(componentId, edge))
                        )
                    )
            yield archetype

  /** Removes a [[Component]] from an entity. The entity is going to move
    * archetypes if the component is present in this archetype.
    * @param entityId
    *   the entity ID.
    * @param componentId
    *   the component ID.
    * @return
    *   an effect of the operation, [F] of [[ComponentArchetype]] if successful.
    */
  def removeComponent(
      entityId: EntityId,
      componentId: ComponentId
  ): F[ComponentArchetype[F]] =
    buffer.get(componentId) match
      case Some(dimension) =>
        edges.get(componentId) match
          case Some(edge) =>
            Sync[F]
              .pure(edge.remove)
              .flatTap(_.moveEntity(this, entityId))
          case None =>
            findArchetype(components - componentId)
              .flatTap(arch =>
                Sync[F]
                  .pure(ArchetypeEdge[F](add = this, remove = arch))
                  .flatTap(_.remove.moveEntity(this, entityId))
                  .flatTap(edge => Sync[F].delay(edges.put(componentId, edge)))
              )
      case None =>
        Sync[F].pure(this)

  // /** Inserts a set of distinct [[Component]] within the same type into a set of
  //   * distinct entities. The entities are going to move archetypes if the
  //   * component ID is not present in this archetype.
  //   * @param componentId
  //   *   the component ID.
  //   * @param entries
  //   *   the entries to insert.
  //   */
  // def addComponentBulk[C <: Component](
  //     componentId: ComponentId,
  //     entries: (EntityId, C)*
  // ): F[ComponentArchetype[F]] =
  //   buffer.get(componentId) match
  //     case Some(dimension) =>
  //       Sync[F]
  //         .pure(this)
  //         .flatTap(_ =>
  //           Sync[F].delay {
  //             entries.foreach { (entityId, component) =>
  //               dimension.put(entityId.toBits, component)
  //             }
  //           }
  //         )
  //     case None =>
  //       for
  //         existingEdge <- Sync[F].pure(edges.get(componentId))
  //         archetype <- existingEdge match
  //           case Some(edge) =>
  //             Sync[F]
  //               .pure(edge.add)
  //               .flatTap(_.addComponentBulk(componentId, entries: _*))
  //           case None =>
  //             findArchetype(components + componentId)
  //               .flatTap(arch =>
  //                 Sync[F]
  //                   .pure(ArchetypeEdge[F](add = arch, remove = this))
  //                   .flatTap(_.add.moveEntityBulk(this, entries.map(_._1): _*))
  //                   .flatTap(edge =>
  //                     Sync[F].delay(edges.put(componentId, edge))
  //                   )
  //               )
  //       yield archetype

  // /** Removes a set of distinct [[Component]] within the same type from a set of
  //   * distinct entities. The entities are going to move archetypes if the
  //   * component ID is present in this archetype.
  //   * @param componentId
  //   *   the component ID.
  //   * @param entities
  //   *   the entities to remove.
  //   * @return
  //   *   the new archetype id.
  //   */
  // def removeComponentBulk(
  //     componentId: ComponentId,
  //     entities: EntityId*
  // ): F[ComponentArchetype[F]] =
  //   buffer.get(componentId) match
  //     case Some(dimension) =>
  //       edges.get(componentId) match
  //         case Some(edge) =>
  //           Sync[F]
  //             .pure(edge.remove)
  //             .flatTap(_.moveEntityBulk(this, entities: _*))
  //         case None =>
  //           findArchetype(components - componentId)
  //             .flatTap(arch =>
  //               Sync[F]
  //                 .pure(ArchetypeEdge[F](add = this, remove = arch))
  //                 .flatTap(edge =>
  //                   edge.remove.moveEntityBulk(this, entities: _*)
  //                 )
  //                 .flatTap(edge => Sync[F].delay(edges.put(componentId, edge)))
  //             )
  //     case None => Sync[F].pure(this)
