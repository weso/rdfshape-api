package controllers

sealed abstract class InputType

case object ByInput extends InputType {
  override def toString = "byInput"
}
case object ByFile extends InputType {
  override def toString = "byFile"
}
case object ByUri extends InputType {
  override def toString = "byUri"
}
case object ByEndpoint extends InputType {
  override def toString = "byEndpoint"
}
case object ByDereference extends InputType {
  override def toString = "byDereference"
}



