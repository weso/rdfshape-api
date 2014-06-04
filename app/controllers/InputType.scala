package controllers

sealed abstract class InputType

case object ByInput extends InputType
case object ByFile extends InputType
case object ByUri extends InputType
case object ByEndpoint extends InputType
case object ByDereference extends InputType



