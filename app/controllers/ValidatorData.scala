package controllers

import scala.Stream
import scala.concurrent.Future
import scala.util.{ Failure => TryFailure, Success => TrySuccess, Try }

import DataOptions.DEFAULT_SHOW_DATA
import SchemaOptions.{ DEFAULT_CUT, DEFAULT_ShowSchema }
import es.weso.rdf.{ PrefixMap, RDFBuilder }
import es.weso.rdf.jena.RDFAsJenaModel
import es.weso.rdf.nodes.IRI
import es.weso.schema._
import es.weso.utils.CommonUtils.getWithRecoverFunction
import es.weso.utils.RDFUtils
import es.weso.utils.RDFUtils.parseStrAsRDFReader
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ Action, AnyContent, Controller }

/**
 * Data + Schema together
 */
trait ValidatorData { this: Controller =>
  
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
    rdfs:Boolean = false,
    schemaName: String): Action[AnyContent] = {
    println(s"validatorData.data: schemaName: $schemaName, dataFormat: $dataFormat")  
    val dataOptions = DataOptions(
      format = RDFUtils.getFormat(Some(dataFormat)), 
      showData = true,
      rdfs)
    val opts_schema = SchemaOptions.default

    val trySchema = for {
      schema <- Schemas.fromString(data, dataFormat,schemaName)
    } yield (schema)

    trySchema match {
      case TrySuccess(schema) => {
        println("Only data?")
        validate_get(data,
          Some(dataFormat),
          dataOptions.showData,
          rdfs,
          schemaName,
          None,
          DEFAULT_CUT,
          DEFAULT_ShowSchema)
      }
      case TryFailure(e) =>
        Action.async { _ => Future(BadRequest(views.html.errorPage(e.getMessage))) }
        // BadRequest(views.html.errorPage(e.getMessage))
      }
  }

  def validate_get(
    dataStr: String, 
    dataFormat: Option[String], 
    showData: Boolean,
    rdfs:Boolean = false,
    schemaName: String, 
    opt_iri: Option[String], 
    cut: Int, 
    showSchema: Boolean
    ): Action[AnyContent] = Action.async {
    validate_get_Future(dataStr,
      dataFormat,
      showData,
      rdfs,
      schemaName,
      opt_iri,
      cut,
      showSchema).map(vrf => {
        vrf match {
          case TrySuccess(vr) => {
            val vf = ValidationForm.fromResult(vr)
            Ok(views.html.validate_data(vr, vf))
          }
          case TryFailure(e) => BadRequest(views.html.errorPage(e.getMessage))
        }
      })
  }

  def validate_get_Future(
    dataStr: String,
    formatData: Option[String],
    showData:Boolean,
    rdfs:Boolean,
    schemaName: String,
    opt_iri: Option[String],
    cut: Int,
    showSchema: Boolean): Future[Try[ValidationResult]] = {
      val iri = opt_iri.map(str => IRI(str))
      val dataOptions = DataOptions(
        format = RDFUtils.getFormat(formatData), 
           showData = showData, 
           rdfs
      )
      val trigger = opt_iri match {
        case None => ValidationTrigger.default
        case Some(str) => ValidationTrigger.nodeAllShapes(str)
      }
      val opts_schema = SchemaOptions(cut = cut, trigger = trigger, showSchema)
      
      parseStrAsRDFReader(dataStr, dataOptions.format, dataOptions.rdfs) match {
        case TrySuccess(data) =>
          scala.concurrent.Future(
          TrySuccess(
            ValidationResult.validateTogether(
              data,
              dataStr,
              dataOptions,
              schemaName, 
              opts_schema)))
        case TryFailure(e) =>
        scala.concurrent.Future(TrySuccess(
          ValidationResult(
              status = Some(false),
              msg = "Error parsing Data with syntax " + dataOptions.format + ": " + e.getMessage,
              result = Result.empty,
              nodes = List(),
              dataStr = dataStr,
              dataOptions = dataOptions,
              withSchema = false,
              schemaStr = "",
              schemaFormat = Schemas.defaultSchemaFormat,
              schemaName = schemaName,
              schemaOptions = opts_schema,
              together = true)))
    }
  }




  def validate_post = Action.async { request =>
    {
      val pair = for (
        vf <- getValidationForm(request); 
        str_data <- vf.dataInput.getDataStr
      ) yield (vf, str_data)

      scala.concurrent.Future {
        pair match {
          case TrySuccess((vf, dataStr)) => {
            println(s"validate_post validation form: $vf")
            val tryValidate =
              for (
                data <- vf.dataInput.getData(vf.dataOptions.format, vf.dataOptions.rdfs); 
                str_schema <- vf.schemaInput.getSchemaStr
              ) yield {
                println(s"validate_post: vf $vf")
                ValidationResult.validateTogether(
                  rdf = data,
                  dataStr = dataStr,
                  dataOptions = vf.dataOptions,
                  schemaName = vf.schemaName,
                  schemaOptions = vf.schemaOptions)
              }
            val vr = getWithRecoverFunction(tryValidate, recoverValidationResult(dataStr, vf))
            Ok(views.html.validate_data(vr, vf))
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
      true)
  }

}

object ValidatorData extends Controller with ValidatorData 