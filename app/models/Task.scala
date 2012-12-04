package models

import play.api.db._
import play.api.Play.current

case class Task(
	id: Long, 
	label: String
)

object Task {

  import anorm._
  import anorm.SqlParser._

  val task = {
	get[Long]("id") ~ 
  	get[String]("label") map {
  	  case id~label => Task(id, label)
  	}
  }
  
  def all(): List[Task] = DB.withConnection { implicit c =>
  	SQL("select * from task").as(task *)
  }
  
def create(label: String) {
  DB.withConnection { implicit c =>
    SQL("insert into task (label) values ({label})").on(
      'label -> label
    ).executeUpdate()
  }
}

	def delete(id: Long) {
		DB.withConnection { implicit c =>
    	SQL("delete from task where id = {id}").on(
    		'id -> id
    		).executeUpdate()
		}
	}

}