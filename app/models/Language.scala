package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Language(
	id: Pk[Long], 
	langCode: String,
    langName: String
)

object Language {

  val lang = {
	get[Pk[Long]]("id") ~ get[String]("langCode") ~ get[String]("langName") map {
  	  case id~langCode~langName => Language(id, langCode,langName)
  	}
  }
  
  def all(): List[Language] = DB.withConnection { implicit c =>
  	SQL("select * from language").as(lang *)
  }
  
  def insert(language : Language) {
	  DB.withConnection { implicit c =>
	  	SQL("insert into language (langCode,langName) values ('%s', '%s')".
	  	     format(language.langCode,
	  	            language.langName)).executeUpdate()
	  }
  }

  def delete(id: Pk[Long]) {
		DB.withConnection { implicit c =>
    	SQL("delete from language where id = {id}").on(
    		'id -> id
    		).executeUpdate()
		}
  }

  def lookup(langCode : String) : Option[Long] = {
    DB.withConnection { implicit c =>
    SQL("select id from language where langCode = {langCode}").on(
    		'langCode -> langCode
    		).as(scalar[Long].singleOpt)
	}
  }
  
  def findLangCode(id : Long) : Option[String] = {
    val found = DB.withConnection { implicit c =>
    SQL("select * from language where id = {id}").on(
    		'id -> id
    	).as(lang *)
	}
    if (found.isEmpty) None
    else Some(found.head.langCode)
  }

  def find(id : Long) : Option[Language] = {
    val found = DB.withConnection { implicit c =>
    SQL("select * from language where id = {id}").on(
    		'id -> id
    	).as(lang *)
	}
    if (found.isEmpty) None
    else Some(found.head)
  }

}
