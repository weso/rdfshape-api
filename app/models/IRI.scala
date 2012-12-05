package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class IRI(
	id: Long, 
	iriName: String
)

object IRI {


  val iri = {
	get[Long]("id") ~ 
  	get[String]("iriName") map {
  	  case id~iriName => IRI(id, iriName)
  	}
  }
  
  def all(): List[IRI] = DB.withConnection { implicit c =>
  	SQL("select * from iri").as(iri *)
  }
  
def create(iriName: String) {
  DB.withConnection { implicit c =>
    SQL("insert into iri (iriName) values ({iriName})").on(
      'iriName -> iriName
    ).executeUpdate()
  }
}

def delete(id: Long) {
		DB.withConnection { implicit c =>
    	SQL("delete from iri where id = {id}").on(
    		'id -> id
    		).executeUpdate()
		}
	}

}