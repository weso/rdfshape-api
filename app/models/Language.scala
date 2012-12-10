package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Language(
	id: Long, 
	langCode: String,
    langName: String
)

object Language {


  val lang = {
	get[Long]("id") ~ get[String]("langCode") ~ get[String]("langName") map {
  	  case id~langCode~langName => Language(id, langCode,langName)
  	}
  }
  
  def all(): List[Language] = DB.withConnection { implicit c =>
  	SQL("select * from language").as(lang *)
  }
  
  def create(langCode: String, langName: String) {
	  DB.withConnection { implicit c =>
	  	SQL("insert into language (langCode,langName) values ('%s', '%s')".
	  	     format(langCode,
	  	            langName)).executeUpdate()
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
