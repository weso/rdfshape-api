package es.weso.rdfshape.server.utils.numeric

import scala.util.Try

/** Utilities related to math
  */
case object NumericUtils {

  /** Try to parse an integer
    *
    * @param str Text chain to be parsed
    * @return The resulting integer if the string was parsed, an error message otherwise
    */
  def parseInt(str: String): Either[String, Int] =
    Try(str.toInt).map(Right(_)).getOrElse(Left(s"$str is not a number"))

}
