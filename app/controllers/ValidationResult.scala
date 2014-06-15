package controllers

import es.weso.shex._
import es.weso.monads.{Failure => FailureMonads}
import es.weso.parser.PrefixMap
import xml.Utility.escape
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import es.weso.rdf.RDFTriples
import com.hp.hpl.jena.sparql.function.library.e
import scala.util._
import es.weso.rdf.RDF

case class ValidationResult(
      status: Option[Boolean]
    , msg: String
    , rs: Stream[Typing]
    , nodes: List[RDFNode]
    , str_rdf: String
    , withSchema : Boolean
    , str_schema: String
    , opt_schema: SchemaOptions
    , pm: PrefixMap
    ) {

  def typing2Html(typing: Typing): String = {
    val sb = new StringBuilder
    sb.append("<tr><th>Node</th><th>Shape</th></tr>")
    for ((node,ts) <- typing.map) {
      sb.append("<tr><td>" + node2Html(node) + 
    		  		 "</td><td>" + nodes2Html(ts) + "</td></tr>")
    }
    sb.toString
  }
  
 def nodes2Html(nodes: Set[RDFNode]): String = {
     val sb = new StringBuilder
     sb.append("<ul class=\"nodes\">")
     for (node <- nodes) {
       sb.append("<li>" + node2Html(node) + "</li>")
     }
     sb.append("</ul>")
     sb.toString
   }

   def node2Html(node: RDFNode): String = {
     if (node.isIRI) ("<a href=\"" + node.toIRI.str + "\">" + escape(node.toIRI.str) + "</a>")
     else escape(node.toString)
   }
    
   def toHTML(cut: Int): String = {
    val sb = new StringBuilder
    for ((t,n) <- rs.take(cut) zip (1 to cut)) {
      val nodesWithoutTyping = nodes.filter(n => t.hasType(n) != Set()).toSet
      sb.append("<h2 class='shapes'>Result " + n + "</h2>")
      sb.append("<table>")
      sb.append(typing2Html(t))
      sb.append("</table>")
      if (!nodesWithoutTyping.isEmpty) {
    	  sb.append("<p>Nodes without shapes</p>")
    	  nodes2Html(nodesWithoutTyping)
      }
    }
    sb.toString
  }
  
  // Conversions to generate permalinks
  def schema_param : Option[String] = {
    if (withSchema) Some(str_schema)
    else None
  }
  
  def opt_iri_param : Option[String] = {
    opt_schema.opt_iri.map(_.str) 
  }
}

object ValidationResult {
  def empty = 
    ValidationResult(None,"",Stream(), List(),"", false, "", SchemaOptions.default, PrefixMap.empty)
  
  def failure(e: Throwable, str_rdf: String, opt_schema: Option[String], opt_iri: Option[IRI]) : ValidationResult = {
    ValidationResult(Some(false),e.getMessage, Stream(),List(),str_rdf, false, "", SchemaOptions.default, PrefixMap.empty)
  }

  def withMessage(msg: String, str_rdf: String, opt_schema: Option[String], opt_iri: Option[IRI]) : ValidationResult = {
    ValidationResult(Some(false),msg, Stream(),List(),str_rdf,false,"",SchemaOptions.default,PrefixMap.empty)
  }

  def validateIRI(
        iri : IRI
      , rdf: RDF
      , str_rdf: String
      , schema: Schema
      , str_schema: String
      , so: SchemaOptions
      , pm: PrefixMap
      ): ValidationResult = {
  val rs = Schema.matchSchema(iri,rdf,schema,so.withIncoming,so.openClosed,so.withAny)
  if (rs.isValid) {
   	 ValidationResult(Some(true),"Shapes found",rs.run,List(iri),str_rdf,true, str_schema, so,pm)
  } else {
     ValidationResult(Some(false),"No shapes found",rs.run,List(iri),str_rdf,true, str_schema,so,pm)
  } 
 }

  def validateAny(
        rdf: RDF
      , str_rdf: String
      , schema: Schema
      , str_schema: String
      , so: SchemaOptions
      , pm: PrefixMap
      ): ValidationResult = {
 val nodes = rdf.subjects.toList
 val rs = Schema.matchAll(rdf,schema,so.withIncoming,so.openClosed,so.withAny)
 if (rs.isValid) {
   ValidationResult(Some(true),"Shapes found",rs.run,nodes,str_rdf,true, str_schema,so,pm)
 } else {
   ValidationResult(Some(false),"No shapes found",rs.run,nodes,str_rdf,true,str_schema,so,pm)
 }
}     

  // TODO: Refactor the following code...
 def validate(
        rdf: RDF
      , str_rdf: String
      , withSchema : Boolean
      , str_schema : String
      , so: SchemaOptions 
      ): ValidationResult = {
   if (withSchema) {
         Schema.fromString(str_schema) match {
         case Success((schema,pm)) => {
               so.opt_iri match {
                 case Some(iri) => validateIRI(iri,rdf,str_rdf,schema,str_schema,so,pm) 
                 case None => validateAny(rdf,str_rdf,schema,str_schema,so,pm)
               }
         }
         case Failure(e) => {
             ValidationResult(Some(false),"Schema did not parse: " + e.getMessage,Stream(),List(),str_rdf,true, str_schema,so,PrefixMap.empty)
         } 
       } 
    } else  
     ValidationResult(Some(true),"RDF parsed",Stream(),List(),str_rdf,false,str_schema,so,PrefixMap.empty)
 } 

}


