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
package basalt.core.query

import basalt.core.datatype.Component
import basalt.core.syntax.all._

import scala.annotation.tailrec
import scala.quoted.{Expr, Quotes, Type}

trait QueryingFilterIterable extends Product with Iterable[QueryingFilter] {
  override def toString: String = this match {
    case head |: Fin  => head.toString
    case head |: tail => s"$head |: $tail"
    case Fin          => "Fin"
  }
}

object QueryingFilterIterable {
  def apply: Fin.type = Fin

  def apply[C <: QueryingFilter: QueryingFilterTag](filter: C): C |: Fin =
    filter |: Fin
}

// The only reason to not use a "case object" extending ComponentSet directly is for
// syntax sugar; using Fin instead of Fin.type.
sealed trait Fin extends QueryingFilterIterable {
  def |:[C <: QueryingFilter: QueryingFilterTag](head: C): C |: Fin =
    basalt.core.syntax.filters.|:(head, this)
}

case object Fin extends Fin {
  override def iterator = Iterator.empty
}

trait QueryingFilterIterableTag[I <: QueryingFilterIterable] {
  def tags: Seq[QueryingFilterTag[QueryingFilter]]
}

inline given deriveQueryingFilterIterableTag[I <: QueryingFilterIterable]
    : QueryingFilterIterableTag[I] = ${ deriveQueryingFilterIterableTagImpl[I] }

private def deriveQueryingFilterIterableTagImpl[
    I <: QueryingFilterIterable: Type
](using quotes: Quotes): Expr[QueryingFilterIterableTag[I]] =
  import quotes.reflect.*
  val repr = TypeRepr.of[I]
  if repr =:= TypeRepr.of[Nothing] then
    report.error(
      "QueryingFilterIterableTag must refer to a list of querying filters."
    )
  else if repr =:= TypeRepr.of[QueryingFilterIterable] then
    report.error(
      "Can't derive QueryingFilterIterableTag for objects not known at compile-time."
    )
  else if hasDuplicates[I] then
    report.error(
      "Can't derive QueryingFilterIterable for sets with duplicate filters."
    )
  '{
    new QueryingFilterIterableTag[I] {
      override def tags: Seq[QueryingFilterTag[QueryingFilter]] =
        getFilterTags[I]
      override def toString: String = tags.toString
      override def hashCode: Int    = tags.hashCode
      override def equals(that: Any): Boolean = that match {
        case that: QueryingFilterIterableTag[_] =>
          tags.sorted == that.tags.sorted
        case _ => false
      }
    }
  }

inline private def getFilterTags[I <: QueryingFilterIterable]
    : Seq[QueryingFilterTag[QueryingFilter]] = {
  import scala.compiletime.erasedValue
  inline erasedValue[I] match {
    case _: (head |: tail) =>
      summon[QueryingFilterTag[head]]
        .asInstanceOf[QueryingFilterTag[QueryingFilter]] +: getFilterTags[tail]
    case _ => Seq()
  }
}

@tailrec
private def hasDuplicates[I <: QueryingFilterIterable: Type](using
    quotes: Quotes
): Boolean =
  Type.of[I] match {
    case '[head |: tail] => hasRepetitions[I, head] || hasDuplicates[tail]
    case _               => false
  }

def hasRepetitions[
    I <: QueryingFilterIterable: Type,
    F <: QueryingFilter: Type
](using quotes: Quotes): Boolean =
  def count[I <: QueryingFilterIterable: Type, F <: QueryingFilter: Type]: Int =
    Type.of[I] match {
      case '[F |: tail] => 1 + count[tail, F]
      case '[_ |: tail] => count[tail, F]
      case _            => 0
    }
  count[I, F] > 1
