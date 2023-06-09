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
package basalt.core.syntax
package filters

import basalt.core.query.{
  QueryingFilter,
  QueryingFilterIterable,
  QueryingFilterTag
}
import scala.annotation.showAsInfix
import basalt.core.query.Fin

trait FilterIterableSyntax:
  extension [
      H <: QueryingFilter: QueryingFilterTag,
      T <: QueryingFilterIterable
  ](head: H)
    def |:(tail: T): basalt.core.syntax.filters.|:[H, T] =
      basalt.core.syntax.filters.|:(head, tail)
  export filters.|:
  export basalt.core.query.{
    deriveQueryingFilterTag,
    deriveQueryingFilterIterableTag,
    orderingForQueryingFilterTag,
    orderingForQueryingFilter,
    Fin
  }

@showAsInfix
final case class |:[
    +C <: QueryingFilter: QueryingFilterTag,
    +L <: QueryingFilterIterable
](h: C, t: L)
    extends QueryingFilterIterable {
  override def iterator: Iterator[QueryingFilter] =
    new Iterator[QueryingFilter] {
      private var remaining: QueryingFilterIterable = |:(h, t)
      override def hasNext: Boolean                 = remaining != Fin
      override def next(): QueryingFilter = {
        val head |: tail = remaining: @unchecked
        remaining = tail
        head
      }
    }
}

object all extends FilterIterableSyntax
