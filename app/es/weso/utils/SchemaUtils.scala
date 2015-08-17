package es.weso.utils

import es.weso.shacl.SchemaFormats

object SchemaUtils {

  val defaultSchemaFormat = "SHEXC"
  
  def getSchemaFormat(format: Option[String]): String = {
    format match {
      case Some(s) => 
        if (SchemaFormats.available(s)) s
        else // TODO: Check a better failure... 
          throw new Exception("Unsupported schema format " + s)
      case None => defaultSchemaFormat
    }
    
  }

}