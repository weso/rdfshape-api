package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Lang(
	id: Long, 
	langCode: String,
    langName: String
)

object Lang {


  val lang = {
	get[Long]("id") ~ get[String]("langCode") ~ get[String]("langName") map {
  	  case id~langCode~langName => Lang(id, langCode,langName)
  	}
  }
  
  def all(): List[Lang] = DB.withConnection { implicit c =>
  	SQL("select * from lang").as(lang *)
  }
  
  def create(langCode: String, langName: String) {
	  DB.withConnection { implicit c =>
	  	SQL("insert into lang (langCode, langName) values ({langCode,langName})").on(
	  		'langCode -> langCode,
	  		'langName -> langName
	  	).executeUpdate()
	  }
  }

  def delete(id: Long) {
		DB.withConnection { implicit c =>
    	SQL("delete from lang where id = {id}").on(
    		'id -> id
    		).executeUpdate()
		}
  }

}
