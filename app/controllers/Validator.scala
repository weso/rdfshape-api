package controllers

import scala.concurrent._
import scala.concurrent.duration._
import akka.actor._

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import java.io.ByteArrayInputStream
import org.apache.commons.io.FileUtils
import play.api._
import play.api.mvc._
import play.api.libs.Files._
import scala.util.{ Try, Success => TrySuccess, Failure => TryFailure }
import es.weso.rdf._
import es.weso.rdfgraph.nodes.IRI
import es.weso.rdf.jena._
// import es.weso.monads.{ Result => SchemaResult, Failure => SchemaFailure, Passed }
import es.weso.utils._
import es.weso.utils.CommonUtils._
import es.weso.utils.RDFUtils._
import es.weso.utils.IOUtils._
import java.net.URL
import java.io.File
import DataOptions._
import SchemaOptions._
import es.weso.shacl._
import es.weso.shacl.converter.RDF2Schema
import es.weso.shacl.converter.Schema2RDF

trait Validator { this: Controller =>

  import Multipart._

  /*  def onlyData(data: String, dataFormat: String, schemaVersion: String) = {
    validate_get(
        data, 
        Some(dataFormat), 
        DEFAULT_SHOW_DATA, None, None, schemaVersion, None, DEFAULT_CUT, false)
  } */

  def data(
    data: String,
    dataFormat: String,
    schemaVersion: String): Action[AnyContent] = {
    // Create a shapes graph and join it to the RDF data
    val rdf: RDFBuilder = RDFAsJenaModel.empty
    val opts_data = DataOptions(
      format = RDFUtils.getFormat(Some(dataFormat)), showData = true)
    val opts_schema = SchemaOptions.default

    val trySchema = for {
      (schema, pm) <- Schema.fromString(data, dataFormat)
    } yield (schema)
    trySchema match {
      case TrySuccess(schema) => {
        validate_get(data,
          Some(dataFormat),
          DEFAULT_SHOW_DATA,
          Some(schema.serialize("SHEXC")),
          Some("SHEXC"),
          schemaVersion,
          None,
          DEFAULT_CUT,
          DEFAULT_ShowSchema)
      }
      case TryFailure(e) =>
        ???
      }
  }

  def dataSchema(
    data: String,
    dataFormat: String,
    schema: String,
    schemaFormat: String,
    schemaVersion: String): Action[AnyContent] = {
    validate_get(data,
      Some(dataFormat),
      DEFAULT_SHOW_DATA,
      Some(schema),
      Some(schemaFormat),
      schemaVersion,
      None,
      DEFAULT_CUT,
      DEFAULT_ShowSchema)
  }

  def dataSchemaNode(
    data: String,
    dataFormat: String,
    schema: String,
    schemaFormat: String,
    schemaVersion: String,
    node: String) = {
    validate_get(data,
      Some(dataFormat),
      DEFAULT_SHOW_DATA,
      Some(schema),
      Some(schemaFormat), schemaVersion, Some(node),
      DEFAULT_CUT,
      DEFAULT_ShowSchema)
  }

  def validate_rdf_get_Future(
    str_data: String,
    formatData: Option[String],
    showData: Boolean,
    schemaVersion: String,
    opt_iri: Option[String],
    cut: Int,
    showSchema: Boolean): Future[Try[ValidationResult]] = {
    val iri = opt_iri.map(str => IRI(str))
    val opts_data = DataOptions(
      format = RDFUtils.getFormat(formatData), showData = showData)
    val opts_schema = SchemaOptions(
      cut = cut, opt_iri = iri, showSchema)
    RDFParse(str_data, opts_data.format) match {
      case TrySuccess(rdf) => {
        scala.concurrent.Future(
          for {
            (schema, pm) <- RDF2Schema.rdf2Schema(rdf)
          } yield ValidationResult.validate(
            rdf,
            str_data,
            opts_data,
            true,
            schema.serialize("SHEXC"),
            "SHEXC",
            schemaVersion, opts_schema))
      }
      case TryFailure(e) =>
        scala.concurrent.Future(TrySuccess(
          ValidationResult(Some(false),
            "Error parsing Data with syntax " + opts_data.format + ": " + e.getMessage,
            Seq(),
            List(),
            str_data,
            opts_data,
            true,
            "",
            SchemaFormats.default,
            schemaVersion,
            opts_schema,
            PrefixMap.empty)))
    }
  }

  def validate_get_Future(
    str_data: String,
    formatData: Option[String],
    showData: Boolean,
    opt_schema: Option[String],
    maybeSchemaFormat: Option[String],
    schemaVersion: String,
    opt_iri: Option[String],
    cut: Int,
    showSchema: Boolean): Future[Try[ValidationResult]] = {
    val withSchema = opt_schema.isDefined
    val iri = opt_iri.map(str => IRI(str))
    val schemaFormat = maybeSchemaFormat.getOrElse(SchemaUtils.defaultSchemaFormat)
    val str_schema = opt_schema.getOrElse("")
    val opts_data = DataOptions(
      format = RDFUtils.getFormat(formatData), showData = showData)

    val opts_schema = SchemaOptions(
      cut = cut, opt_iri = iri, showSchema)
    RDFParse(str_data, opts_data.format) match {
      case TrySuccess(data) =>
        scala.concurrent.Future(
          TrySuccess(
            ValidationResult.validate(
              data,
              str_data,
              opts_data,
              withSchema,
              str_schema,
              schemaFormat, schemaVersion, opts_schema)))
      case TryFailure(e) =>
        scala.concurrent.Future(TrySuccess(
          ValidationResult(Some(false),
            "Error parsing Data with syntax " + opts_data.format + ": " + e.getMessage,
            Seq(),
            List(),
            str_data,
            opts_data,
            withSchema,
            str_schema,
            schemaFormat,
            schemaVersion,
            opts_schema,
            PrefixMap.empty)))
    }
  }

  // TODO: Simplify this ugly code...long list of arguments
  def validate_get(
    str_data: String, 
    dataFormat: Option[String], 
    showData: Boolean, 
    opt_schema: Option[String], 
    schemaFormat: Option[String], 
    schemaVersion: String, 
    opt_iri: Option[String], 
    cut: Int, 
    showSchema: Boolean
    ) = Action.async {
    validate_get_Future(str_data,
      dataFormat,
      showData,
      opt_schema,
      schemaFormat,
      schemaVersion,
      opt_iri,
      cut,
      showSchema).map(vrf => {
        vrf match {
          case TrySuccess(vr) => {
            val vf = ValidationForm.fromResult(vr)
            Ok(views.html.index(vr, vf))
          }
          case TryFailure(e) => BadRequest(views.html.errorPage(e.getMessage))
        }
      })
  }

  // TODO: Simplify this ugly code...long list of arguments
  def validate_rdf_get(
    str_data: String,
    dataFormat: Option[String],
    showData: Boolean,
    schemaVersion: String,
    opt_iri: Option[String], 
    cut: Int, 
    showSchema: Boolean): Action[AnyContent] = Action.async {
    validate_rdf_get_Future(str_data,
      dataFormat,
      showData,
      schemaVersion,
      opt_iri,
      cut,
      showSchema).map(vrf => {
        vrf match {
          case TrySuccess(vr) => {
            val vf = ValidationForm.fromResult(vr)
            Ok(views.html.index(vr, vf))
          }
          case TryFailure(e) => 
            BadRequest(views.html.errorPage(e.getMessage))
        }
      })
  }

  def validate_post = Action.async { request =>
    {

      val pair = for (
        vf <- getValidationForm(request); str_data <- vf.dataInput.getDataStr
      ) yield (vf, str_data)

      scala.concurrent.Future {
        pair match {
          case TrySuccess((vf, str_data)) => {
            val tryValidate =
              for (
                data <- vf.dataInput.getData(vf.dataOptions.format); str_schema <- vf.schemaInput.getSchemaStr
              ) yield {
                ValidationResult.validate(
                  data,
                  str_data,
                  vf.dataOptions,
                  vf.withSchema,
                  str_schema,
                  vf.schemaInput.inputFormat,
                  vf.schemaInput.schemaVersion.versionName,
                  vf.schemaOptions)
              }
            val vr = getWithRecoverFunction(tryValidate, recoverValidationResult(str_data, vf))
            Ok(views.html.index(vr, vf))
          }
          case TryFailure(e) => BadRequest(views.html.errorPage(e.getMessage))
        }
      }
    }
  }

  def recoverValidationResult(str_data: String, vf: ValidationForm)(e: Throwable): ValidationResult = {
    val schema_str: String = Try(vf.schemaInput.getSchemaStr.get).getOrElse("")
    ValidationResult(
      Some(false),
      e.getMessage(),
      Stream(),
      List(),
      str_data,
      vf.dataOptions,
      vf.withSchema,
      schema_str,
      vf.schemaInput.inputFormat,
      vf.schemaInput.schemaVersion.versionName,
      vf.schemaOptions,
      PrefixMap.empty)
  }

}

object Validator extends Controller with Validator 