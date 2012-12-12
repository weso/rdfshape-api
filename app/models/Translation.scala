package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Translation(
	id: Pk[Long],
	iriId : Long, 
	langId: Long,
    transLabel: String,
    votes : Long
)

object Translation {

  val trans = {
	get[Pk[Long]]("id") ~ 
	get[Long]("iriId") ~ 
	get[Long]("langId") ~ 
	get[String]("transLabel") ~ 
	get[Long]("votes") map {
  	  case id ~
  	  	   iriId ~
  	  	   langId ~
  	  	   transLabel ~
  	  	   votes => {
  	  	     val tr = Translation(id, iriId,langId, transLabel, votes)
  	  	     tr
  	  	   }
  	}
  }
  
  
  def all(): List[Translation] = DB.withConnection { implicit c =>
  	SQL("select * from translation").as(trans *)
  }
  
  def create(iriId: Long, langId: Long, transLabel: String, votes : Int = 1) {
    DB.withConnection { implicit c =>
      SQL("insert into translation (iriId,langId,transLabel,votes) values (%s, %s, '%s', %s)".
	  	     format(iriId,langId,transLabel,votes)).executeUpdate()
    }
  }

  def insert(translation : Translation) {
	  DB.withConnection { implicit c =>
	  	SQL("insert into translation (iriId,langId,transLabel,votes) values (%s, %s,'%s',%s)".
	  	     format(translation.iriId,
	  	            translation.langId,
	  	            translation.transLabel,
	  	            translation.votes)).executeUpdate()
	  }
  }
  def delete(id: Pk[Long]) {
		DB.withConnection { implicit c =>
    	SQL("delete from trans where id = {id}").on(
    		'id -> id
    		).executeUpdate()
		}
	}

  def lookup(iriName : String, langCode : String) : Option[String] = {
    	DB.withConnection { implicit c =>
    		SQL( """
    		    SELECT t.transLabel
    				FROM translation t
    				INNER JOIN language l ON l.id = t.langid
    				INNER JOIN iri i ON i.id = t.iriid
    				WHERE (l.langCode = '{langCode}' and i.iriName='{iriName}' );""""
    		    ).on(
    		   'langCode -> langCode,
    		   'iriName -> iriName
    		  ).as(scalar[String].singleOpt)
		}
  }


    def lookupIds(iriId : Long, langId: Long) : Option[Long] = {
        val query = "SELECT id FROM translation WHERE { iriId = %s and langId = %s } ORDER BY votes DESC;".format(iriId,langId)
        val ids : List[Long]= DB.withConnection { implicit c =>
    		SQL(query).as(scalar[Long].*)
		}
        if (ids.isEmpty) None
        else Some(ids.head)
  }
    
  def lookupTranslation(iriStr : String, langCode : String) : Option[Translation] = {
    for { idIRI <- IRI.lookup(iriStr)
          idLang <- Language.lookup(langCode)
          id <- Translation.lookupIds(idIRI,idLang)
          tr <- Translation.findById(id)
    } yield tr
  }
  
  def findById(id : Long) : Option[Translation] = {
   val transs = DB.withConnection { implicit c =>
  		 SQL("select * from translation where id = {id}").on('id -> id).as(trans *)
       }
   if (transs.isEmpty) None
   else Some(transs.head)
  }

}