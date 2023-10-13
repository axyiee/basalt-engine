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
package basalt.core

import archetype.ComponentArchetype
import collection.GenerationalKey
import datatype.{Component, ComponentSet}
import default.BasaltComponentView
import descriptor.{
  ArchetypesDescriptor,
  EntitiesDescriptor,
  ComponentsDescriptor
}
import event.internal.InternalEvent
import query.{ComponentFilterTag, QueryingFilter, QueryingFilterTag}
import syntax.all.given
import syntax.all._

import cats.effect._
import cats.syntax.all._

import munit._

import fs2.concurrent.Topic
import basalt.core.collection.GenerationalVector

case class Sample1(val value: Int)    extends Component
case class Sample2(val value: String) extends Component
case class Sample3(val value: Float)  extends Component

private def initComponentView: IO[BasaltComponentView[IO]] =
  for
    components <- ComponentsDescriptor[IO]
    entities = new EntitiesDescriptor[IO]
    archetypes <- ArchetypesDescriptor[IO]
    events     <- Ref[IO].of[List[InternalEvent]](List.empty)
  yield BasaltComponentView(components, entities, archetypes, events)

class ArchetypeSuite extends CatsEffectSuite:
  test("Archetypes store multiple components per entity") {
    for
      components <- initComponentView
      entityId   <- components.entities.init
      _          <- components.set(entityId, Sample1(1))
      _          <- components.set(entityId, Sample2("Hallo Welt!"))
      _          <- components.set(entityId, Sample3(3.1415f))
      assertion <- assertIO(
        components.extractAll(entityId).compile.to(Set),
        Set(Sample1(1), Sample2("Hallo Welt!"), Sample3(3.1415f)),
        "Archetype retrieved a mismatching set of components"
      )
    yield assertion
  }
  test("Archetypes store multiple entities") {
    for
      components <- initComponentView
      entity1    <- components.entities.init
      entity2    <- components.entities.init
      _ <- components
        .set(entity1, Sample1(2))
        .flatTap(_ => components.set(entity2, Sample1(4)))
      _ <- components
        .set(entity1, Sample2("Hallo Welt! [3.1415]"))
        .flatTap(_ => components.set(entity2, Sample2("Hallo Welt! [2.42]")))
      _ <- components
        .set(entity1, Sample3(3.1415f))
        .flatTap(_ => components.set(entity2, Sample3(2.42f)))
      assertion <- assertIO(
        components.extractAll(entity1).compile.to(Set),
        Set(
          Sample1(2),
          Sample2("Hallo Welt! [3.1415]"),
          Sample3(3.1415f)
        ),
        "Archetype retrieved a mismatching set of components for entity 1"
      ) *> assertIO(
        components.extractAll(entity2).compile.to(Set),
        Set(
          Sample1(4),
          Sample2("Hallo Welt! [2.42]"),
          Sample3(
            2.42f
          )
        ),
        "Archetype retrieved a mismatching set of components for entity 2"
      )
    yield assertion
  }
  test(
    "Entities get their archetype switched when adding/removing components"
  ) {
    for
      components <- initComponentView
      entityId   <- components.entities.init
      old        <- components.entities.liftArchetypeId(entityId)
      _          <- components.set(entityId, Sample2("Hallo Welt!"))
      `new`      <- components.entities.liftArchetypeId(entityId)
      assertion <- IO.delay(
        assertNotEquals(
          old,
          `new`,
          "Archetype was not changed when adding a component"
        )
      )
    yield assertion
  }
