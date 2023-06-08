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
package basalt.core

import archetype.ComponentArchetype
import collection.GenerationalKey
import datatype.{Component, ComponentSet}
import descriptor.{ArchetypesDescriptor, ComponentsDescriptor}
import query.{ComponentFilterTag, QueryingFilter, QueryingFilterTag}
import syntax.all.given
import syntax.all._

import cats.effect._
import cats.syntax.all._
import munit._

case class Sample1(val value: Int)    extends Component
case class Sample2(val value: String) extends Component
case class Sample3(val value: Float)  extends Component

class ArchetypeSuite extends CatsEffectSuite:
  test("Archetypes can store multiple components per entity") {
    val x = Sample1(1) <> (Sample2("Hallo Welt!") <> (Sample3(3.1415f) <> Fin))
    for
      components <- ComponentsDescriptor[IO]
      archetypes <- ArchetypesDescriptor[IO]
      archetype <- components
        .register[Sample1 <> (Sample2 <> (Sample3 <> Fin))]
        .flatMap(archetypes.apply)
      entity <- IO.pure(GenerationalKey(1))
      _ <- components
        .getId[Sample1]
        .flatMap(archetype.addComponent(entity, _, Sample1(1)))
      _ <- components
        .getId[Sample2]
        .flatMap(archetype.addComponent(entity, _, Sample2("Hallo Welt!")))
      _ <- components
        .getId[Sample3]
        .flatMap(archetype.addComponent(entity, _, Sample3(3.1415f)))
      assertion <- assertIOBoolean(
        IO.pure(Sample1(1) :: Sample2("Hallo Welt!") :: Sample3(3.1415f) :: Nil)
          .map(list => archetype.getComponents(entity).forall(list.contains)),
        "Archetype retrieved a mismatching set of components"
      )
    yield assertion
  }
  test("Archetypes can store multiple entities") {
    for
      components <- ComponentsDescriptor[IO]
      archetypes <- ArchetypesDescriptor[IO]
      archetype <- components
        .register[Sample1 <> (Sample2 <> (Sample3 <> Fin))]
        .flatMap(archetypes.apply)
      entity1 <- IO.pure(GenerationalKey(1))
      entity2 <- IO.pure(GenerationalKey(2, 5))
      _ <- components
        .getId[Sample1]
        .flatMap(
          archetype
            .addComponentBulk(_, (entity1, Sample1(2)), (entity2, Sample1(4)))
        )
      _ <- components
        .getId[Sample2]
        .flatMap(
          archetype.addComponentBulk(
            _,
            (entity1, Sample2("Hallo Welt! [3.1415]")),
            (entity2, Sample2("Hallo Welt! [2.42]"))
          )
        )
      _ <- components
        .getId[Sample3]
        .flatMap(
          archetype.addComponentBulk(
            _,
            (entity1, Sample3(3.1415f)),
            (entity2, Sample3(2.42f))
          )
        )
      assertion <- assertIOBoolean(
        IO.pure(
          Sample1(2) :: Sample2("Hallo Welt! [3.1415]") :: Sample3(
            3.1415f
          ) :: Nil
        ).map(list => archetype.getComponents(entity1).forall(list.contains)),
        "Archetype retrieved a mismatching set of components for entity 1"
      ) *> assertIOBoolean(
        IO.pure(
          Sample1(4) :: Sample2("Hallo Welt! [2.42]") :: Sample3(2.42f) :: Nil
        ).map(list => archetype.getComponents(entity2).forall(list.contains)),
        "Archetype retrieved a mismatching set of components for entity 2"
      )
    yield assertion
  }
