package controllers

import es.weso.monads.{ Failure => FailureMonads }
import es.weso.rdf._
import xml.Utility.escape
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.nodes.IRI
import es.weso.rdf.RDFTriples
import com.hp.hpl.jena.sparql.function.library.e
import scala.util._
import es.weso.rdf._
import play.Logger
import es.weso.utils.SchemaUtils
import es.weso.shacl.SchemaVersions._
import es.weso.shacl.Shacl._
import es.weso.shacl.ValidationError
import es.weso.shacl.ShaclMatcher
import es.weso.shacl.Schema
import es.weso.rdf.validator.{ ValidationResult => ShaclResult, _ }
import es.weso.typing._

case class ValidationResult(
    status: Option[Boolean], 
    msg: String, 
    result: Seq[Map[RDFNode, (Seq[Label], Seq[Label])]], nodes: List[RDFNode], str_data: String, opts_data: DataOptions, withSchema: Boolean, str_schema: String, schema_format: String, schema_version: String, opt_schema: SchemaOptions, pm: PrefixMap) {

  type Result_ = Seq[SingleResult]
  type SingleResult = Map[RDFNode, (Seq[Label], Seq[Label])]

  def result2Html(results: Result_): String = {
    println("Results: " + results)
    val sb = new StringBuilder
    val cut = opt_schema.cut
    for ((result, n) <- results zip (1 to cut)) {
      println("Result in looop: " + result)
      sb.append("<h2 class='result'>Result" + printNumber(n, cut) + "</h2>")
      sb.append("<table class='result'>")
      sb.append(singleResult2Html(result))
      sb.append("</table>")
    }
    sb.toString
  }

  def singleResult2Html(result: SingleResult): String = {
    println("SingleResult: " + result)
    val sb = new StringBuilder
    sb.append("<tr><th>Node</th><th>Shapes</th></tr>")
    for ((node, pairs) <- result.toSeq) {
      sb.append("<tr><td>" + node2Html(node) +
        "</td><td>" + labels2Html(pairs._1, true) +
        labels2Html(pairs._2, false) + "</td>" +
        "</tr>")
    }
    sb.toString
  }

  def labels2Html(nodes: Seq[Label], isPositive: Boolean): String = {
    val sb = new StringBuilder
    val cls = if (isPositive) "positiveLabels"
              else "negativeLabels"
                
    if (isPositive == false && !nodes.isEmpty) {
      sb.append("Negative Shapes")
    }
    sb.append(s"""<ul class='$cls'>""")
    for (node <- nodes) {
      sb.append(s"""<li class="labels">${label2Html(node)}</li>""")
    }
    sb.append("</ul>")
    sb.toString
  }

  def label2Html(label: Label): String = {
    node2Html(label.getNode())
  }

  def node2Html(node: RDFNode): String = {
    if (node.isIRI) code(node.toIRI.toString)
    else code(node.toString)
  }

  def code(str: String): String = {
    s"""<code>${escape(str)}</code>"""
  }

  def toHTML(): String = {
    result2Html(result)
  }

  def printNumber(n: Int, cut: Int): String = {
    if (n == 1 && cut == 1) ""
    else n.toString
  }
  // Conversions to generate permalinks
  def schema_param: Option[String] = {
    if (withSchema) Some(str_schema)
    else None
  }

  def opt_iri_param: Option[String] = {
    opt_schema.opt_iri.map(_.str)
  }
}

object ValidationResult {
  // TODO: refactor the following ugly code
  def empty =
    ValidationResult(None,
      "",
      Stream(),
      List(), "",
      DataOptions.default, false,
      "",
      SchemaUtils.defaultSchemaFormat,
      defaultSchemaVersion,
      SchemaOptions.default,
      PrefixMap.empty)

  def validateIRI(
    iri: IRI,
    data: RDFReader,
    str_data: String,
    dataOptions: DataOptions,
    schema: Schema,
    str_schema: String,
    schema_format: String,
    schemaOptions: SchemaOptions,
    pm: PrefixMap): ValidationResult = {
    val matcher = ShaclMatcher(schema, data)
    val result = matcher.match_node_AllLabels(iri)
    val (ok, msg, next) = extractResult(result)
    ValidationResult(Some(ok), msg, next, List(iri), str_data, dataOptions, true, str_schema, schema_format, defaultSchemaVersion, schemaOptions, pm)
  }

  def validateAny(
    data: RDFReader, str_data: String, dataOptions: DataOptions, schema: Schema, str_schema: String, schema_format: String, schemaOptions: SchemaOptions, pm: PrefixMap): ValidationResult = {
    val nodes = data.subjects.toList
    val validator = ShaclMatcher(schema, data)
    val result = validator.matchAllNodes_AllLabels
    val (ok, msg, next) = extractResult(result)
    ValidationResult(Some(ok), msg, next, nodes, str_data, dataOptions, true, str_schema, schema_format, defaultSchemaVersion, schemaOptions, pm)
  }

  def extractResult(result: ShaclResult[RDFNode, Label, Throwable]): (Boolean, String, Seq[Map[RDFNode, (Seq[Label], Seq[Label])]]) = {
    println("Extracting result from " + result)
    result.extract match {
      case Failure(e)     => (false, s"Validation Error: $e", Seq())
      case Success(Seq()) => (false, s"No results", Seq())
      case Success(rs)    => (true, s"Shapes", rs)
    }
  }

  // TODO: Refactor the following code...
  def validate(
    rdf: RDFReader, str_data: String, dataOptions: DataOptions, withSchema: Boolean, str_schema: String, schema_format: String, schema_version: String, schemaOptions: SchemaOptions): ValidationResult = {
    if (withSchema) {
      Try(Schema.fromString(str_schema).get) match {
        case Success((schema, pm)) => {
          schemaOptions.opt_iri match {
            case Some(iri) => validateIRI(iri, rdf, str_data, dataOptions, schema, str_schema, schema_format, schemaOptions, pm)
            case None      => validateAny(rdf, str_data, dataOptions, schema, str_schema, schema_format, schemaOptions, pm)
          }
        }
        case Failure(e) => {
          Logger.info("Schema did not parse..." + e.getMessage)
          ValidationResult(Some(false),
            "Schema did not parse: " + e.getMessage,
            Stream(), List(), str_data, dataOptions, true,
            str_schema, schema_format, schema_version, schemaOptions,
            PrefixMap.empty)
        }
      }
    } else
      ValidationResult(Some(true), "RDF parsed",
        Stream(), List(), str_data, dataOptions, false,
        str_schema, schema_format, schema_version, schemaOptions,
        PrefixMap.empty)
  }

}


