package controllers

import scala.util.{ Failure, Success }

import es.weso.rdf.RDFReader
import es.weso.rdf.nodes.{ IRI, RDFNode }
import es.weso.schema._
import play.Logger

case class ValidationResult(
    status: Option[Boolean],  // 
    msg: String,
    result: Result,
    nodes: List[RDFNode],
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

  def schemaFormat_param: Option[String] = {
    Some(schemaFormat)
  }

  def maybeFocusNode: Option[String] = {
    schemaOptions.maybeFocusNode
  }
  
  def toHTML: String = {
    result.toHTML(schemaOptions.cut)
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
    str_data: String,
    dataOptions: DataOptions,
    schema: Schema,
    str_schema: String,
    schema_format: String,
    schemaOptions: SchemaOptions,
    together: Boolean
    ): ValidationResult = {
    
    val result = schema.validateNodeAllLabels(iri,data)
    val ok = result.isValid
    val msg = result.message
    ValidationResult(Some(ok),
      msg,
      result,
      List(iri),
      str_data, 
      dataOptions, 
      true, 
      str_schema, 
      schema_format, 
      schema.name, 
      schemaOptions,
      together)
  }

  /**
   * Creates a validation result when validating any node
   */
  def validateAny(
    data: RDFReader, 
    str_data: String, 
    dataOptions: DataOptions, 
    schema: Schema, 
    str_schema: String, 
    schema_format: String, 
    schemaOptions: SchemaOptions,
    together: Boolean
    ): ValidationResult = {
    val nodes = data.subjects.toList
    val result = schema.validateAllNodesAllLabels(data)
    ValidationResult(Some(result.isValid), 
        result.message, 
        result, 
        nodes, 
        str_data, 
        dataOptions, 
        true, 
        str_schema, 
        schema_format, 
        schema.name, 
        schemaOptions,
        together)
  }

  /**
   * Creates a validation result when validating any node
   */
  def validateScopeNodes(
    data: RDFReader, 
    str_data: String, 
    dataOptions: DataOptions, 
    schema: Schema, 
    str_schema: String, 
    schema_format: String, 
    schemaOptions: SchemaOptions,
    together: Boolean
    ): ValidationResult = {
    val nodes = data.subjects.toList
    val result = schema.validateRDF(data)
    ValidationResult(Some(result.isValid), 
        result.message, 
        result, 
        nodes, 
        str_data, 
        dataOptions, 
        true, 
        str_schema, 
        schema_format, 
        schema.name, 
        schemaOptions,
        together)
  }

  /**
   * Creates a validation result when validating data & schema together
   */
  def validateTogether(
    rdf: RDFReader, 
    str_data: String, 
    dataOptions: DataOptions, 
    schemaName: String, 
    schemaOptions: SchemaOptions
    ): ValidationResult = {
     println(s"validating when they are together..., schemaName: $schemaName")
     val trySchema = Schemas.fromRDF(rdf,schemaName)
     trySchema match {
        case Success(schema) => {
          val format = dataOptions.format
          val result = schema.validateRDF(rdf)
          val strSchema = schema.serialize(format).getOrElse("")
          ValidationResult(
              Some(result.isValid), 
                   result.message, 
                   result, 
                   List(), 
                   str_data, 
                   dataOptions, 
                   false, 
                   strSchema, 
                   format, 
                   schema.name, 
                   schemaOptions,
                   true)
        }
        case Failure(e) =>
          ValidationResult(Some(false),
            s"Schema did not parse : ${e.getMessage}, name: $schemaName",
            Result.empty, List(), str_data, dataOptions, false,
            "", Schemas.defaultSchemaFormat, schemaName, schemaOptions,true
            )
      }
  }
  
  /**
   * Creates a validation result when validating data + schema (separated)
   */
  def validateDataSchema(
    rdf: RDFReader, 
    str_data: String, 
    dataOptions: DataOptions, 
    withSchema: Boolean, 
    strSchema: String, 
    schemaFormat: String, 
    schemaName: String, 
    schemaOptions: SchemaOptions): ValidationResult = {
    println(s"ValidationResult.validate...withSchema: $withSchema")

    if (withSchema) {
      Schemas.fromString(strSchema,schemaFormat,schemaName,None) match {
        case Success(schema) =>
          validate_withSchema(rdf,str_data,dataOptions,schema,schemaOptions)
        case Failure(e) => {
          Logger.info("Schema did not parse..." + e.getMessage)
          ValidationResult(Some(false),
            s"Schema did not parse : ${e.getMessage}, name: $schemaName",
            Result.empty, List(), str_data, dataOptions, true,
            strSchema, schemaFormat, schemaName, schemaOptions,false
            )
        }
      }
    } 
   else {
      ValidationResult(Some(true), "RDF parsed",
        Result.empty, List(), str_data, dataOptions, false,
        strSchema, schemaFormat, schemaName, schemaOptions,false)
    }
  }

  /**
   * Creates a validation result when validating data with a Schema
   */
  def validate_withSchema(
    rdf: RDFReader, 
    str_data: String, 
    dataOptions: DataOptions, 
    schema: Schema, 
    schemaOptions: SchemaOptions): ValidationResult = {
    val schemaFormat = schema.defaultFormat
    val strSchema = schema.serialize(schemaFormat).getOrElse("")
    schemaOptions.trigger match {
       case NodeAllShapes(node) => validateIRI(node.toIRI, rdf, str_data, dataOptions, schema, strSchema, schemaFormat, schemaOptions,false)
       case ScopeDeclarations => validateScopeNodes(rdf, str_data, dataOptions, schema, strSchema, schemaFormat, schemaOptions,false)
       case AllNodesAllShapes => validateAny(rdf, str_data, dataOptions, schema, strSchema, schemaFormat, schemaOptions,false)
       case t => ValidationResult(Some(false),
            s"Unsupported validation trigger $t",
            Result.empty, List(), str_data, dataOptions, true,
            strSchema, schemaFormat, schema.name, schemaOptions,false
            )
 
    }
  }

}
