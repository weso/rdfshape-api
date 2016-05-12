package controllers

import scala.Stream
import scala.concurrent.Future
import scala.util.{ Failure => TryFailure, Success => TrySuccess, Try }

import DataOptions.DEFAULT_SHOW_DATA
import SchemaOptions.{ DEFAULT_CUT, DEFAULT_ShowSchema }
import es.weso.rdf.{ PrefixMap, RDFBuilder }
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.IRI
// import es.weso.schema.SchemaUtils
// import es.weso.shex.{ Schema => ShExSchema, SchemaFormat => ShExSchemaFormat}
// import es.weso.shex.converter.RDF2Schema
import es.weso.schema._
import es.weso.utils.CommonUtils.getWithRecoverFunction
import es.weso.utils.RDFUtils
import es.weso.utils.RDFUtils.parseStrAsRDFReader
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ Action, AnyContent, Controller }

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
    schemaName: String): Action[AnyContent] = {
    // Create a shapes graph and join it to the RDF data
    val rdf: RDFBuilder = RDFAsJenaModel.empty
    val opts_data = DataOptions(
      format = RDFUtils.getFormat(Some(dataFormat)), showData = true)
    val opts_schema = SchemaOptions.default

    val trySchema = for {
      schema <- Schemas.fromString(data, dataFormat,schemaName)
    } yield (schema)

    trySchema match {
      case TrySuccess(schema) => {
        validate_get(data,
          Some(dataFormat),
          DEFAULT_SHOW_DATA,
          Some(data),
          Some(dataFormat),
          schemaName,
          None,
          DEFAULT_CUT,
          DEFAULT_ShowSchema)
      }
      case TryFailure(e) =>
        throw new Exception("Validator Exception in data: " + e)
      }
  }

  def dataSchema(
    data: String,
    dataFormat: String,
    schema: String,
    schemaFormat: String,
    schemaName: String): Action[AnyContent] = {
    validate_get(data,
      Some(dataFormat),
      DEFAULT_SHOW_DATA,
      Some(schema),
      Some(schemaFormat),
      schemaName,
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

  /**
   * Validate RDF only
   */
  def validate_rdf_get_Future(
    str_data: String,
    formatData: Option[String],
    showData: Boolean,
    schemaName: String,
    opt_iri: Option[String],
    cut: Int,
    showSchema: Boolean): Future[Try[ValidationResult]] = {
    val iri = opt_iri.map(str => IRI(str))
    val opts_data = DataOptions(
      format = RDFUtils.getFormat(formatData), showData = showData)
    val opts_schema = SchemaOptions(
      cut = cut, opt_iri = iri, showSchema)
    parseStrAsRDFReader(str_data, opts_data.format) match {
      case TrySuccess(rdf) => {
        scala.concurrent.Future(
          for {
            schema <- Schemas.fromRDF(rdf,schemaName)
          } yield 
          ValidationResult.validate_withSchema(
            rdf,
            str_data,
            opts_data,
            schema, 
            opts_schema))
      }
      case TryFailure(e) =>
        scala.concurrent.Future(TrySuccess(
          ValidationResult(Some(false),
            "Error parsing Data with syntax " + opts_data.format + ": " + e.getMessage,
            Result.empty,
            List(),
            str_data,
            opts_data,
            true,
            "",
            opts_data.format,
            schemaName,
            opts_schema)))
    }
  }

  def validate_get_Future(
    str_data: String,
    formatData: Option[String],
    showData: Boolean,
    opt_schema: Option[String],
    maybeSchemaFormat: Option[String],
    schemaName: String,
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
      
    parseStrAsRDFReader(str_data, opts_data.format) match {
      case TrySuccess(data) =>
        scala.concurrent.Future(
          TrySuccess(
            ValidationResult.validate(
              data,
              str_data,
              opts_data,
              withSchema,
              str_schema,
              schemaFormat, schemaName, opts_schema)))
      case TryFailure(e) =>
        scala.concurrent.Future(TrySuccess(
          ValidationResult(Some(false),
            "Error parsing Data with syntax " + opts_data.format + ": " + e.getMessage,
            Result.empty,
            List(),
            str_data,
            opts_data,
            withSchema,
            str_schema,
            schemaFormat,
            schemaName,
            opts_schema)))
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
                data <- vf.dataInput.getData(vf.dataOptions.format); 
                str_schema <- vf.schemaInput.getSchemaStr
              ) yield {
                ValidationResult.validate(
                  data,
                  str_data,
                  vf.dataOptions,
                  vf.withSchema,
                  str_schema,
                  vf.schemaInput.inputFormat,
                  vf.schemaInput.schemaName,
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
      Result.empty,
      List(),
      str_data,
      vf.dataOptions,
      vf.withSchema,
      schema_str,
      vf.schemaInput.inputFormat,
      vf.schemaInput.schemaName,
      vf.schemaOptions)
  }

}

object Validator extends Controller with Validator 