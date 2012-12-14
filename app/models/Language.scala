package models

import play.api.db._
import play.api.Play.current
import play.api.Logger

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
  
  def create(langCode : String, langName: String) {
    // Only creates a new language if it didn't exist
    if (lookup(langCode) == None)
	  DB.withConnection { implicit c =>
	  	SQL("insert into language (langCode,langName) values ('%s', '%s')".
	  	     format(langCode,
	  	            langName)).executeUpdate()
	  }
  }
  
  def insert(language : Language) {
    if (lookup(language.langCode) == None)
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
    val ids = DB.withConnection { implicit c =>
    		  SQL("select id from language where langCode = {langCode}").on(
    				  'langCode -> langCode
    		  ).as(scalar[Long].*)
    		}
    ids.length match {
      case 0 => None
      case 1 => Some(ids.head)
      case _ => {
        Logger.warn("Lookup lang: " + langCode + ". More than one id (selected the first)")
        println("Lookup lang: " + langCode + ". More than one id (selected the first)")
        Some(ids.head)
      }
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
