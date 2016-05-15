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

/**
 * Schema and Data separated
 */
trait ValidatorSchemaData { this: Controller =>
  
  import Multipart._

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
        format = RDFUtils.getFormat(formatData), showData = showData
      )
      val trigger = ValidationTrigger.fromOptIRI(opt_iri)
      val opts_schema = SchemaOptions(cut = cut, trigger = trigger, showSchema)
      
      parseStrAsRDFReader(str_data, opts_data.format) match {
        case TrySuccess(data) =>
          scala.concurrent.Future(
          TrySuccess(
            ValidationResult.validateDataSchema(
              data,
              str_data,
              opts_data,
              withSchema,
              str_schema,
              schemaFormat, 
              schemaName, 
              opts_schema)))
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
            opts_schema,
            false)))
    }
  }

  def validate_get(
    str_data: String, 
    dataFormat: Option[String], 
    showData: Boolean, 
    opt_schema: Option[String], 
    schemaFormat: Option[String], 
    schemaName: String, 
    opt_iri: Option[String], 
    cut: Int, 
    showSchema: Boolean
    ) = Action.async {
    println("validate_get: opt_schema..." + opt_schema)
    validate_get_Future(str_data,
      dataFormat,
      showData,
      opt_schema,
      schemaFormat,
      schemaName,
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
                ValidationResult.validateDataSchema(
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

  def recoverValidationResult(str_data: String, 
      vf: ValidationForm)(e: Throwable): ValidationResult = {
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
      vf.schemaOptions,
      false)
  }

}

object ValidatorSchemaData extends Controller with ValidatorSchemaData 