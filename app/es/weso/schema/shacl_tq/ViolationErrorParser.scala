package es.weso.schema.shacl_tq

import es.weso.rdf.nodes._
import es.weso.rdf.parser.RDFParser
import util._

object ViolationErrorParser extends RDFParser {
  
  lazy val sh = IRI("http://www.w3.org/ns/shacl#")
  lazy val sh_message = sh + "message"
  lazy val sh_focusNode = sh + "focusNode"
  lazy val sh_subject = sh + "subject"
  lazy val sh_predicate = sh + "predicate"
  lazy val sh_severity = sh + "severity"
  lazy val sh_sourceConstraint = sh + "sourceConstraint"
  lazy val sh_sourceShape = sh + "sourceShape"
  lazy val sh_sourceTemplate = sh + "sourceTemplate"
  
  
  
  def parse: RDFParser[ViolationError] = { (n,rdf) =>
    for {
      message <- stringFromPredicate(sh_message)(n,rdf)
    ; focusNode <- objectFromPredicateOptional(sh_focusNode)(n,rdf)
    ; subject <- objectFromPredicateOptional(sh_subject)(n,rdf)
    ; predicate <- objectFromPredicateOptional(sh_predicate)(n,rdf)
    ; severity <- objectFromPredicateOptional(sh_severity)(n,rdf)
    ; sourceConstraint <- objectFromPredicateOptional(sh_sourceConstraint)(n,rdf)
    ; sourceShape <- objectFromPredicateOptional(sh_sourceShape)(n,rdf)
    ; sourceTemplate <- objectFromPredicateOptional(sh_sourceTemplate)(n,rdf)
    } yield 
       ViolationError(message,focusNode, subject,predicate,severity,sourceConstraint,sourceShape,sourceTemplate)
  }
  

}