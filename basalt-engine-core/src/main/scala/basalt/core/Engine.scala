package basalt.core

/** An overview and supervisor of all entities, components, attributes,
  * resources, and their associated metadata.
  *
  * Each [Entity] has an unique identification and a set of components and
  * attributes. Each component must be associated with a type used up to a
  * single time on each [Entity]. Entities and components can be either created,
  * removed, updated, or queried using a given [Engine] implementation which
  * supervises the environment of a specific platform implementation.
  *
  * ==Attributes==
  *
  * [Attribute]s are a derivation of components often used to save resources or
  * store data temporarily within the RAM. Those aren't serialized unless
  * manually specified otherwise. Attributes are by default, data already
  * provided by a platform implementation - such as hunger, health, display
  * name, etc. - therefore not worth the data persistance.
  *
  * ==Resources==
  *
  * [Resource]s are unique instances of a data type stored independent from
  * entities. Those are available in all contexts, to every entity, and are used
  * for storing global context or data that is not tied to any specific entity,
  * such as configurations or game states.
  */
trait Engine[F[_]] {}
