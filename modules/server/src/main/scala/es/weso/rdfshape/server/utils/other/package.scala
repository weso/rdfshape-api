package es.weso.rdfshape.server.utils

import cats.implicits.catsSyntaxEitherId
import io.circe._

package object other {

  /** Given a decoding operation whose result may contain an error, map the
    * erroring result to a [[DecodingFailure]] containing it as message
    * If a native decoding failure occurred, leave it as is, else if an error
    * occurred return it as a decoding failure , else return the value
    *
    * @param input Either resulting of a Circe decode operation
    * @tparam L Left type of input either
    * @tparam R Right type of input Either
    * @return A [[Decoder.Result]] containing a Decoding failure or an item of type [[R]]
    * @note This method fulfills a specific goal for this app, mainly
    */
  def mapEitherToDecodeResult[L, R](
      input: Either[DecodingFailure, Either[L, R]]
  ): Either[DecodingFailure, R] = {
    input.flatMap {
      case Left(err)    => DecodingFailure(err.toString, Nil).asLeft
      case Right(value) => value.asRight
    }
  }
}
