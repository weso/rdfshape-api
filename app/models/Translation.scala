package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Translation(
	id: Long,
	iriId : Long, 
	langId: Long,
    transLabel: String,
    votes : Int
)

object Translation {


  val trans = {
	get[Long]("id") ~ 
	get[Long]("iriId") ~ 
	get[Long]("langId") ~ 
	get[String]("transLabel") ~ 
	get[Int]("votes") map {
  	  case id ~
  	  	   iriId ~
  	  	   langId ~
  	  	   transLabel ~
  	  	   votes => {
  	  	     val tr = Translation(id, iriId,langId, transLabel, votes)
  	  	     println("Get trans = " + tr)
  	  	     tr
  	  	   }
  	}
  }
  
  def all(): List[Translation] = DB.withConnection { implicit c =>
  	println("Hello from Trans...all")
  	SQL("select * from translation").as(trans *)
  }
  
def create(iriId: Long, langId: Long, transLabel: String, votes : Int = 1) {
  DB.withConnection { implicit c =>
    SQL("insert into language (iriId,langId,transLabel,votes) values (%s, %s, '%s', %s)".
	  	     format(iriId,langId,transLabel,votes)).executeUpdate()
  }
}

def delete(id: Long) {
		DB.withConnection { implicit c =>
    	SQL("delete from trans where id = {id}").on(
    		'id -> id
    		).executeUpdate()
		}
	}

}