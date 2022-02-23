package es.weso.rdfshape.server.utils.numeric

import scala.util.{Random, Try}

/** Utilities related to math
  */
case object NumericUtils {

  /** Random generator to help creating random amounts
    */
  private lazy val random = Random

  /** Try to parse an integer
    *
    * @param str Text chain to be parsed
    * @return The resulting integer if the string was parsed, an error message otherwise
    */
  def parseInt(str: String): Either[String, Int] =
    Try(str.toInt).map(Right(_)).getOrElse(Left(s"$str is not a number"))

  /** Generate a random integer in a given range
    *
    * @param min Lower limit (inclusive)
    * @param max Upper limit (exclusive)
    * @return A random Int in range [min-max)
    */
  def getRandomInt(min: Int = 0, max: Int = 10): Int = {
    random.between(min, max)
  }

}
