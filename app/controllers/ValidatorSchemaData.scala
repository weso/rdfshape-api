package controllers

import scala.Stream
import scala.concurrent.Future
import util._

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
    rdfs: Boolean = false,
    schema: String,
    schemaFormat: String,
    schemaName: String): Action[AnyContent] = {
    validate_get(data,
      Some(dataFormat),
      DEFAULT_SHOW_DATA,
      rdfs,
      Some(schema),
      Some(schemaFormat),
      schemaName,
      None,
      None,
      ValidationTrigger.default.name,
      DEFAULT_CUT,
      DEFAULT_ShowSchema)
  }

  def dataSchemaNode(
    data: String,
    dataFormat: String,
    rdfs: Boolean = false,
    schema: String,
    schemaFormat: String,
    schemaVersion: String,
    node: String) = {
    validate_get(data,
      Some(dataFormat),
      DEFAULT_SHOW_DATA,
      rdfs,
      Some(schema),
      Some(schemaFormat), 
      schemaVersion, 
      Some(node),
      None,
      ValidationTrigger.default.name,
      DEFAULT_CUT,
      DEFAULT_ShowSchema)
  }

  def validate_get(
    dataStr: String, 
    dataFormat: Option[String], 
    showData: Boolean,
    rdfs: Boolean = false,
    maybeSchema: Option[String], 
    schemaFormat: Option[String], 
    schemaName: String, 
    node: Option[String],
    shape: Option[String],
    trigger: String,
    cut: Int, 
    showSchema: Boolean
    ): Action[AnyContent] = Action.async {
    println(s"validate_get: maybeSchema $maybeSchema, schemaName $schemaName, schemaFormat: $schemaFormat")
    validate_get_Future(dataStr,
      dataFormat,
      showData,rdfs,
      maybeSchema,
      schemaFormat,
      schemaName,
      node,
      shape,
      trigger,
      cut,
      showSchema).map(vrf => {
        vrf match {
          case Success(vr) => {
            val vf = ValidationForm.fromResult(vr)
            Ok(views.html.index(vr, vf))
          }
          case Failure(e) => BadRequest(views.html.errorPage(e.getMessage))
        }
      })
  }
  
  def validate_get_Future(
    dataStr: String,
    dataFormat: Option[String],
    showData: Boolean,
    rdfs: Boolean,
    maybeSchema: Option[String],
    maybeSchemaFormat: Option[String],
    schemaName: String,
    node: Option[String],
    shape: Option[String], 
    trigger: String,
    cut: Int,
    showSchema: Boolean): Future[Try[ValidationResult]] = {
      val withSchema = maybeSchema.isDefined
      val schemaStr = maybeSchema.getOrElse("")
      val iri = node.map(str => IRI(str))
      val schemaFormat = maybeSchemaFormat.getOrElse(SchemaUtils.defaultSchemaFormat)
      val dataOptions = DataOptions(
        format = RDFUtils.getFormat(dataFormat), 
        showData = showData,
        rdfs
      )
      Future{ for {
            validationTrigger <- ValidationTrigger.findTrigger(trigger, node, shape)  
            val schemaOptions = SchemaOptions(cut = cut, trigger = validationTrigger, showSchema)
            rdf <- parseStrAsRDFReader(dataStr, dataOptions.format, rdfs)
          } yield {
            println(s"validation_get_future: Trigger: $trigger")
            ValidationResult.validateDataSchema(
              rdf,
              dataStr,
              dataOptions,
              withSchema,
              schemaStr,
              schemaFormat, 
              schemaName, 
              schemaOptions)
              }
      }
  }

  

  def validate_post = Action.async { request =>
    {
      val pair = for (
        vf <- getValidationForm(request); 
        dataStr <- vf.dataInput.getDataStr
      ) yield (vf, dataStr)

      scala.concurrent.Future {
        pair match {
          case Success((vf, dataStr)) => {
            val tryValidate =
              for (
                data <- vf.dataInput.getData(vf.dataOptions.format, 
                        vf.dataOptions.rdfs) 
              ) yield {
                println(s"validate_post: schemaName = ${vf.schemaName}")
                ValidationResult.validateDataSchema(
                  data,
                  dataStr,
                  vf.dataOptions,
                  vf.withSchema,
                  vf.schemaStr,
                  vf.schemaFormat,
                  vf.schemaName,
                  vf.schemaOptions)
              }
            val vr = getWithRecoverFunction(tryValidate, recoverValidationResult(dataStr, vf))
            Ok(views.html.index(vr, vf))
          }
          case Failure(e) => BadRequest(views.html.errorPage(e.getMessage))
     }
    }
   }
  }

  def recoverValidationResult(str_data: String, 
      vf: ValidationForm)(e: Throwable): ValidationResult = {
    val schema_str: String = vf.schemaInput.getSchemaStr.getOrElse("")
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