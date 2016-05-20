package controllers

import scala.util.{ Try, Failure, Success }

import es.weso.rdf.RDFReader
import es.weso.rdf.nodes.{ IRI, RDFNode }
import es.weso.schema._
import play.Logger
import es.weso.rdf.PrefixMap

case class ValidationResult(
    status: Option[Boolean],  // 
    msg: String,
    result: Result,
    nodes: List[RDFNode], // TODO: Can we remove this one?
    dataStr: String,
    dataOptions: DataOptions,
    withSchema: Boolean,
    schemaStr: String,
    schemaFormat: String,
    schemaName: String,
    schemaOptions: SchemaOptions,
    together: Boolean) {

  // Conversions to generate permalinks
  def showData = dataOptions.showData
  def dataFormat = dataOptions.format
  def cut = schemaOptions.cut
  def showSchema = schemaOptions.showSchema

  def schema_param: Option[String] = {
    if (withSchema) Some(schemaStr)
    else None
  }

  def getSchema: Try[Schema] = {
    for {
      schema <- Schemas.fromString(schemaStr,schemaFormat,schemaName,None)
    } yield schema
  }
  
  def pm: PrefixMap = {
    getSchema.map(_.pm).getOrElse(PrefixMap.empty)
  }

  def schemaFormat_param: Option[String] = {
    Some(schemaFormat)
  }

  def maybeFocusNode: Option[String] = {
    schemaOptions.maybeFocusNode
  }
  
  def toHTML: String = {
    getSchema match {
      case Success(schema) => result.toHTML(schemaOptions.cut,schema)
      case Failure(e) => "Exception trying to get schema " + e.getMessage
    }
  }
  
}

object ValidationResult {
  
  lazy val DefaultTogether = false
  /**
   * Empty validation result
   */
  def empty =
    ValidationResult(
      status = None,
      msg = "",
      result = Result.empty,
      nodes = List(),
      dataStr = "",
      dataOptions = DataOptions.default,
      withSchema = false,
      schemaStr = "",
      schemaFormat = SchemaUtils.defaultSchemaFormat,
      schemaName = Schemas.defaultSchemaName,
      schemaOptions = SchemaOptions.default,
      together = DefaultTogether
      )

  /**
   * Creates a validation result when validating against an IRI
   */
  def validateIRI(
    iri: IRI,
    data: RDFReader,
    dataStr: String,
    dataOptions: DataOptions,
    schema: Schema,
    schemaStr: String,
    schemaFormat: String,
    schemaName: String,
    schemaOptions: SchemaOptions,
    together: Boolean
    ): ValidationResult = {
    val result = schema.validateNodeAllShapes(iri,data)
    val ok = result.isValid
    val msg = result.message
    ValidationResult(Some(ok),
      msg,
      result,
      List(iri),
      dataStr, 
      dataOptions, 
      true, 
      schemaStr, 
      schemaFormat, 
      schemaName, 
      schemaOptions,
      together)
  }

  /**
   * Creates a validation result when validating any node
   */
  def validateAny(
    data: RDFReader, 
    dataStr: String, 
    dataOptions: DataOptions, 
    schema: Schema, 
    schemaStr: String, 
    schemaFormat: String,
    schemaName: String,
    schemaOptions: SchemaOptions,
    together: Boolean
    ): ValidationResult = {
    val nodes = data.subjects.toList
    val result = schema.validateAllNodesAllShapes(data)
    ValidationResult(Some(result.isValid), 
        result.message, 
        result, 
        nodes, 
        dataStr, 
        dataOptions, 
        true, 
        schemaStr, 
        schemaFormat, 
        schemaName, 
        schemaOptions,
        together)
  }

  /**
   * Creates a validation result when validating any node
   */
  def validateScopeNodes(
    data: RDFReader, 
    dataStr: String, 
    dataOptions: DataOptions, 
    schema: Schema, 
    schemaStr: String, 
    schemaFormat: String,
    schemaName: String,
    schemaOptions: SchemaOptions,
    together: Boolean
    ): ValidationResult = {
    val nodes = data.subjects.toList
    val result = schema.validate(data)
    ValidationResult(Some(result.isValid), 
        result.message, 
        result, 
        nodes, 
        dataStr, 
        dataOptions, 
        true, 
        schemaStr, 
        schemaFormat, 
        schemaName, 
        schemaOptions,
        together)
  }

  /**
   * Creates a validation result when validating data & schema together
   */
  def validateTogether(
    rdf: RDFReader, 
    dataStr: String, 
    dataOptions: DataOptions, 
    schemaName: String, 
    schemaOptions: SchemaOptions
    ): ValidationResult = {
     println(s"validating when they are together..., schemaName: $schemaName")
     val trySchema = Schemas.fromRDF(rdf,schemaName)
     trySchema match {
        case Success(schema) => {
          val format = dataOptions.format
          val result = schema.validate(rdf)
          val schemaStr = schema.serialize(format).getOrElse("")
          ValidationResult(
              Some(result.isValid), 
                   result.message, 
                   result, 
                   List(), 
                   dataStr, 
                   dataOptions, 
                   false, 
                   schemaStr, 
                   format, 
                   schemaName, 
                   schemaOptions,
                   true)
        }
        case Failure(e) =>
          ValidationResult(Some(false),
            s"Schema did not parse : ${e.getMessage}, name: $schemaName",
            Result.empty, List(), dataStr, dataOptions, false,
            dataStr, dataOptions.format, schemaName, schemaOptions,true
            )
      }
  }
  
  /**
   * Creates a validation result when validating data + schema (separated)
   */
  def validateDataSchema(
    rdf: RDFReader, 
    dataStr: String, 
    dataOptions: DataOptions, 
    withSchema: Boolean, 
    schemaStr: String, 
    schemaFormat: String, 
    schemaName: String, 
    schemaOptions: SchemaOptions): ValidationResult = {
    println(s"ValidationResult.validate. withSchema: $withSchema schemaName: $schemaName")

    if (withSchema) {
      Schemas.fromString(schemaStr,schemaFormat,schemaName,None) match {
        case Success(schema) =>
          validate_withSchema(rdf,dataStr,dataOptions,schema,schemaStr,schemaFormat,schemaName,schemaOptions)
        case Failure(e) => {
          Logger.info("Schema did not parse..." + e.getMessage)
          ValidationResult(Some(false),
            s"Schema did not parse : ${e.getMessage}, name: $schemaName",
            Result.empty, List(), dataStr, dataOptions, true,
            schemaStr, schemaFormat, schemaName, schemaOptions,false
            )
        }
      }
    } 
   else {
      ValidationResult(Some(true), "RDF parsed",
        Result.empty, List(), dataStr, dataOptions, false,
        schemaStr, schemaFormat, schemaName, schemaOptions,false)
    }
  }

  /**
   * Creates a validation result when validating data with a Schema
   */
  def validate_withSchema(
    rdf: RDFReader, 
    dataStr: String, 
    dataOptions: DataOptions, 
    schema: Schema, 
    schemaStr: String,
    schemaFormat: String,
    schemaName: String,
    schemaOptions: SchemaOptions): ValidationResult = {
    schemaOptions.trigger match {
       case NodeAllShapes(node) => validateIRI(node.toIRI, rdf, dataStr, dataOptions, schema, schemaStr, schemaFormat, schemaName, schemaOptions,false)
       case ScopeDeclarations => validateScopeNodes(rdf, dataStr, dataOptions, schema, schemaStr, schemaFormat, schemaName, schemaOptions,false)
       case AllNodesAllShapes => validateAny(rdf, dataStr, dataOptions, schema, schemaStr, schemaFormat, schemaName, schemaOptions,false)
       case t => ValidationResult(Some(false),
            s"Unsupported validation trigger $t",
            Result.empty, List(), dataStr, dataOptions, true,
            schemaStr, schemaFormat, schemaName, schemaOptions, false
            )
 
    }
  }

}
