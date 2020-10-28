package es.weso.server.merged
import cats._
import cats.data._ 
import cats.implicits._ 

sealed abstract class ActiveDataTab {
    val id: String
}
case object DataTextArea extends ActiveDataTab {
    override val id = "#dataTextArea"
}
case object DataUrl extends ActiveDataTab {
    override val id = "#dataUrl"
}

case object DataEndpoint extends ActiveDataTab {
    override val id = "#dataEndpoint"
}
case object DataFile extends ActiveDataTab {
    override val id = "#dataFile"
}

object ActiveDataTab {
   val values = List(DataTextArea,DataUrl,DataFile,DataEndpoint)
   val default: ActiveDataTab = values.head
 
   def fromString(str: String): Either[String,ActiveDataTab] = {

     values.collectFirst {
       case v if v.id == str => v
     } match {
       case None => Left(s"Unknown value for activeDataTab: $str. Available values: ${values.map(_.id).mkString(",")}")
       case Some(v) => Right(v)
     }
   }
}
