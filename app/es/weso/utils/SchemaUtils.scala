package es.weso.utils

object SchemaUtils {

/*  def schemaParse(str: String, syntax: SchemaSyntax): Try[(Schema,String)] = {
    syntax match {
/*      case SHEXC => {
        // Todo: simplify the following... 
         Try{ 
           RDFTriples.parse(str) match {
         	case Success(rdf) => 
         	  (rdf,str)
         	case Failure(e) =>
         	  throw new Exception("Exception :" + e.getMessage)
           }
         }
      } */
      case _ => 
        Failure(throw new Exception("Not implemented parser for syntax" + syntax + " yet"))
    }
 }
 */

}