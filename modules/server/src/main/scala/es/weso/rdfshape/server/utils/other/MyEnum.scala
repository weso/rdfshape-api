package es.weso.rdfshape.server.utils.other

/** Enumeration with support for free properties and values
  *
  * @tparam T Type contained in the enum
  */
trait MyEnum[T] {

  /** Set of values in the enum
    */
  val values: Set[T]

  /** Default value to be used
    */
  val default: T
}
