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

import syntax.all.given
import datatype.Component
import query.{ComponentFilterTag, QueryingFilter}

import cats.effect._
import munit._

private def getStringRepresentation[C <: Component: ComponentFilterTag](using
    tag: ComponentFilterTag[C]
): String = tag.toString

class QueryingSuite extends CatsEffectSuite:
  test("Querying filter tag can be implicitly summoned by components") {
    class TestComponent(val value: Int) extends Component
    assertEquals(
      getStringRepresentation[TestComponent],
      "basalt.core.QueryingSuite._$TestComponent"
    )
  }
