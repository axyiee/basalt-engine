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
package basalt.core.event
package internal

import basalt.core.datatype.EntityId
import basalt.core.datatype.ComponentId

/** An agnostic type for detecting changes in the state of the engine between
  * ticks.
  */
trait InternalEvent

/** Represents an addition of a component to an entity. Also represents the
  * change of archetype for an entity.
  */
case class ComponentAdded(entityId: EntityId, componentId: ComponentId)
    extends InternalEvent

/** Represents a removal of a component from an entity. Also represents the
  * change of archetype for an entity.
  */
case class ComponentRemoved(entityId: EntityId, componentId: ComponentId)
    extends InternalEvent

/** Represents a change in the value of a already present component for an
  * entity.
  */
case class ComponentUpdated(entityId: EntityId, componentId: ComponentId)
    extends InternalEvent
