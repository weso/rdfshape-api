package models

import play.api.db._
import play.api.Logger
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class IRI(
	id: Pk[Long], 
	iriName: String
)

object IRI {


  val iri = {
	get[Pk[Long]]("id") ~ get[String]("iriName") map {
  	  case id~iriName => IRI(id, iriName)
  	}
  }
  
  def all(): List[IRI] = DB.withConnection { implicit c =>
  	SQL("select * from iri").as(iri *)
  }
  
def create(iriName: String) {
  // Insert an IRI only if it did not exist
  if (lookup(iriName) == None) 
   DB.withConnection { implicit c =>
     SQL("insert into iri (iriName) values ({iriName})").on(
       'iriName -> iriName
     ).executeUpdate()
   }
}

 def delete(id: Pk[Long]) {
  DB.withConnection { implicit c =>
    SQL("delete from iri where id = {id}").on(
    		'id -> id
    		).executeUpdate()
		}
	}

 def lookup(iriName : String) : Option[Long] = {
    val ids = DB.withConnection { implicit c =>
    	SQL("select id from iri where iriName = {iriName}").on(
    		'iriName -> iriName
    		).as(scalar[Long].*)
		}
    ids.length match {
      case 0 => None
      case 1 => Some(ids.head)
      case _ => {
        Logger.warn("Lookup iri: " + iriName + ". More than one id (selected the first)")
        println("Lookup iri: " + iriName + ". More than one id (selected the first)")
        Some(ids.head)
      }
    }
  }
 
   def findIRIName(id : Long) : Option[String] = {
    val found = DB.withConnection { implicit c =>
    SQL("select * from iri where id = {id}").on(
    		'id -> id
    	).as(iri *)
	}
    if (found.isEmpty) None
    else Some(found.head.iriName)
  }

   def find(id : Long) : Option[IRI] = {
    val found = DB.withConnection { implicit c =>
    SQL("select * from iri where id = {id}").on(
    		'id -> id
    	).as(iri *)
	}
    if (found.isEmpty) None
    else Some(found.head)
  }

}