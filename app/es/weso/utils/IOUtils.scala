package es.weso.utils

import java.net.URL
import play.Logger
import scala.util.Failure
import java.io.File
import scala.util.Try
import scala.util.Success

object IOUtils {
    def getURI(uri:String): Try[String] = {
    try {
      Logger.info("Trying to reach " + uri)
      val str = io.Source.fromURL(new URL(uri)).getLines().mkString("\n")
      Success(str)
    } catch {
    case e: Exception => Failure(throw new Exception("getURI: cannot retrieve content from " + uri + "\nException: " + e.getMessage))
    }
  }


  def getFileContents(opt_file: Option[File]):Try[String] = {
     opt_file match {
         case Some(file) => {
           try {
            val str = io.Source.fromFile(file).getLines().mkString("\n")
            Success(str)
           }
           catch {
             case e: Exception => Failure(throw new Exception("getFileContents: cannot retrieve content from file " + file + "\nException: " + e.getMessage))
           }
         } 
     case None => {
        Failure(new Exception("getFileContents: Input by file but no file found"))
     }
   }
  }

  def failMsg[A](msg:String): Try[A] = {
    Failure(throw new Exception(msg))
  }

  def notImplementedYet[A] : Try[A] = 
   Failure(throw new Exception("Not implemented yet"))

}