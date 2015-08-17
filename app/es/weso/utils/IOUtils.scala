package es.weso.utils

import java.net.URL
import play.Logger
import scala.util.{Try, 
  Failure => TryFailure, 
  Success => TrySuccess}
import java.io.File

object IOUtils {
    def getURI(uri:String): Try[String] = {
    try {
      Logger.info("Trying to reach " + uri)
      val str = io.Source.fromURL(new URL(uri)).getLines().mkString("\n")
      TrySuccess(str)
    } catch {
    case e: Exception => TryFailure(throw new Exception("getURI: cannot retrieve content from " + uri + "\nException: " + e.getMessage))
    }
  }


  def getFileContents(opt_file: Option[File]):Try[String] = {
     opt_file match {
         case Some(file) => {
           try {
            val str = io.Source.fromFile(file).getLines().mkString("\n")
            TrySuccess(str)
           }
           catch {
             case e: Exception => TryFailure(throw new Exception("getFileContents: cannot retrieve content from file " + file + "\nException: " + e.getMessage))
           }
         } 
     case None => {
        TryFailure(new Exception("getFileContents: no file found"))
     }
   }
  }

  def failMsg[A](msg:String): Try[A] = {
    TryFailure(throw new Exception(msg))
  }

  def notImplementedYet[A] : Try[A] = 
   TryFailure(throw new Exception("Not implemented yet"))

  /** Transform a string "a\nb\nc" to 1| a\n2| b\n3| c\n"
   */
  def showLineNumbers(str:String):String = {
    val lines = str.split("\n")
    val withNumbers = lines.zip (1 to lines.length)
    val formatted = withNumbers.map(p => f"${p._2}%3d| ${p._1}")
    formatted.mkString("\n")    
  }

}