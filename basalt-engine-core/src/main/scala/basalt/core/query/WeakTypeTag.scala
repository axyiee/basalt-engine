package basalt.core.query

import scala.quoted.{Expr, Quotes, Type}

/** Retrieves the weak type tag for [[T]]. */
def weakTypeTag[T](using tag: WeakTypeTag[T]): WeakTypeTag[T] = tag

/** Brief description about a general-use type. It is used for type comparison
  * scenarios that wouldn't be possible at runtime.
  */
trait WeakTypeTag[F]:
  /** Type comparison between two types that doesn't account type parameters.
    *
    * @return
    *   true if parent type is similar, false otherwise.
    */
  def matches[F: WeakTypeTag](using tag: WeakTypeTag[F]): Boolean

  /** Type comparison that takes account the type parameters.
    *
    * @return
    *   true if parent type and inner types are similar, false otherwise.
    */
  override def equals(other: Any): Boolean

  /** Retrives the first type parameter available.
    *
    * @return
    *   [[Some]] of [[WeakTypeTag]] if it exists, [[None]] otherwise.
    */
  def firstTypeParameter: Option[WeakTypeTag[_]]

inline given deriveWeakTypeTag[F]: WeakTypeTag[F] =
  ${ deriveWeakTypeTagImpl[F] }

private def deriveWeakTypeTagImpl[F: Type](using
    quotes: Quotes
): Expr[WeakTypeTag[F]] =
  import quotes.reflect.*
  val repr = TypeRepr.of[F]

  val typeParameter = repr.typeArgs.headOption match
    case Some(kind) =>
      kind.asType match
        case '[t] => '{ Some(deriveWeakTypeTag[t]) }
        case _    => throw new Exception("unreachable")
    case _ => '{ Option.empty[WeakTypeTag[?]] }

  val qualifiedName = repr.classSymbol.get.fullName
  '{
    new WeakTypeTag[F] { scope =>
      override def firstTypeParameter = ${ typeParameter }

      override def toString      = ${ Expr(qualifiedName) }
      override def hashCode: Int = toString.hashCode

      override def matches[F: WeakTypeTag](using tag: WeakTypeTag[F]) =
        scope.hashCode == tag.hashCode

      override def equals(other: Any) = other match
        case t: WeakTypeTag[?] =>
          matches[t.type] && t.firstTypeParameter == scope.firstTypeParameter
        case _ => false
    }
  }


object WeakTypeTag {
  def apply[T]: WeakTypeTag[T] = weakTypeTag[T]

  def unapply(tag: WeakTypeTag[_]): Option[WeakTypeTag[?]] =
    tag.firstTypeParameter
}