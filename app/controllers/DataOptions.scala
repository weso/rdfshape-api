package controllers

import es.weso.shex._
import es.weso.monads._
import es.weso.rdf._
import es.weso.utils._
import xml.Utility.escape
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import es.weso.shacl.DataFormats
import java.io.File
import play.Logger


case class DataOptions(
   format: String
 , showData: Boolean
 ) 
    
object DataOptions {
  
  // TODO: Check why TRIG doesn't work
  lazy val availableFormats: List[String] = 
    removeList(DataFormats.toList,"TRIG") // TRIG serializer raises an exception
  

  lazy val default : DataOptions =
    DataOptions("TURTLE", true)

  // TODO: Look for a more elegant way to remove an element from a list
  def removeList[A](xs:List[A],x:A): List[A] = 
    xs.patch(xs.indexOf(x),Nil,1)
}
