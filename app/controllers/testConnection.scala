import java.sql.{Connection, DriverManager, ResultSet};

trait db {

  // Quick way to load the driver
  // Class.forName("com.mysql.jdbc.Driver").newInstance

  def getConnectionByUrl(url: String) = {
    DriverManager.getConnection(url)
  }

  def getConnection(name: String) = {
    getConnectionByUrl("jdbc:mysql://localhost:3306/labels4all?user=cursoXML&password=cursoXML")
  }

  /*
  def withConnection[A](block: Connection => A): A = {
    withConnection("default")
  } */

  def withConnection[A](name: String)(block: Connection => A): A = {
    val connection = getConnection(name)
    try {
      block(connection)
    } finally {
      connection.close()
    }
  }

  def withTransaction[A](name: String)(block: Connection => A): A = {
    withConnection(name) { connection =>
      try {
        connection.setAutoCommit(false)
        val r = block(connection)
        connection.commit()
        r
      } catch {
        case e => connection.rollback(); throw e
      }
    }
  }
}
object db extends db {
  def withConnection[A](block: Connection => A): A = {
    this.withConnection("default")(block)
  }

  def withTransaction[A](block: Connection => A): A = {
    this.withTransaction("default")(block)
  }
  
}