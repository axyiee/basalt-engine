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
package basalt.core.query

import basalt.core.datatype.{Attribute, Component}

import scala.quoted.{Expr, Quotes, Type}

/** An object relative to querying. Allows a system to be run as long the filter
  * on the current and previous context are valid.
  */
trait QueryingFilter

/** Brief description about a querying type.
  *
  * @tparam F
  *   the type whose compile-time information should be extracted.
  */
trait QueryingFilterTag[F](
    val qualifiedName: String,
    val isComponentTag: Boolean
):
  override def equals(that: Any): Boolean =
    that.isInstanceOf[QueryingFilterTag[F]] &&
      that.asInstanceOf[QueryingFilterTag[F]].hashCode() == this.hashCode()

inline given deriveQueryingFilterTag[F <: Component | QueryingFilter]
    : QueryingFilterTag[F] =
  ${ deriveQueryingFilterTagImpl[F] }

private def deriveQueryingFilterTagImpl[F: Type](using
    quotes: Quotes
): Expr[QueryingFilterTag[F]] =
  import quotes.reflect.*
  val repr = TypeRepr.of[F]
  val isFilter = repr <:< TypeRepr
    .of[QueryingFilter] && !(repr =:= TypeRepr.of[QueryingFilter])
  val isComponentOrAttribute =
    repr <:< TypeRepr.of[Component] && !(repr =:= TypeRepr
      .of[Component]) && !(repr =:= TypeRepr.of[Attribute])
  if (!isFilter && !isComponentOrAttribute) || (isFilter && isComponentOrAttribute)
  then
    report.error(
      s"${repr.show} must be a **subtype** of **either** basalt.core.datatype.Component or basalt.core.query.QueryingFilter"
    )
  val qualifiedName = repr.classSymbol.get.fullName
  '{
    new QueryingFilterTag[F](
      ${ Expr(qualifiedName) },
      ${ Expr(isComponentOrAttribute) }
    ) {
      override def toString: String = ${ Expr(repr.show) }
      override def hashCode: Int    = ${ Expr(repr.show.hashCode) }
      override def equals(that: Any): Boolean = that match {
        case that: QueryingFilterTag[_] => that.hashCode == this.hashCode
        case _                          => false
      }
    }
  }

given orderingForQueryingFilterTag
    : Ordering[QueryingFilterTag[QueryingFilter]] =
  Ordering.by(_.qualifiedName)
