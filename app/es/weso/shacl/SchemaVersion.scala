package es.weso.shacl

import scala.util._

sealed trait SchemaVersion {
 
 def versionName: String 
 
 override def toString = versionName
}

case object SHACL extends SchemaVersion {
 override def versionName = "SHACL_0.1"
}

case object SHEX_Deriv extends SchemaVersion {
 override def versionName = "SHEX_0.1"
}

object SchemaVersions {

  val availableVersions : List[SchemaVersion] = 
    List(SHACL)
    
  val availableNames : List[String]= 
    availableVersions.map(_.versionName)

  lazy val default = SHACL
  
  lazy val defaultSchemaVersion = default.versionName
  
  def lookup(key:String): Try[SchemaVersion] = {
    availableVersions.find(sv => sv.versionName == key) match {
      case None => Failure(new Exception ("Key " + key + " not found in available schema versions: " + availableNames))
      case Some(x) => Success(x)
    }
  }
  
  def get(key:String): SchemaVersion = {
    availableVersions.find(sv => sv.versionName == key).getOrElse(default)
  }
}