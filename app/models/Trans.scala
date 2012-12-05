package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Trans(
	id: Long,
	iriId : Long, 
	langId: Long,
    transLabel: String,
    votes : Int
)

object Trans {


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
  	  	   votes => Trans(id, iriId,langId, transLabel, votes)
  	}
  }
  
  def all(): List[Trans] = DB.withConnection { implicit c =>
  	SQL("select * from trans").as(trans *)
  }
  
def create(iriId: Long, langId: Long, transLabel: String, votes : Int = 1) {
  DB.withConnection { implicit c =>
    SQL("insert into trans (iriId, langId, transLabel, votes) values ({iriId, langId, transLabel, votes})").on(
      'iriId -> iriId,
      'langId -> langId,
      'transLabel -> transLabel,
      'votes -> votes
    ).executeUpdate()
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