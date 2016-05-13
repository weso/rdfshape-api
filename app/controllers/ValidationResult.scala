package controllers

import scala.util.{ Failure, Success }

import es.weso.rdf.RDFReader
import es.weso.rdf.nodes.{ IRI, RDFNode }
import es.weso.schema.{ Result, Schema, SchemaUtils, Schemas }
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
    schemaOptions.opt_iri.map(_.str)
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
   * Creates a validation result when validating data + schema
   */
  def validate(
    rdf: RDFReader, 
    str_data: String, 
    dataOptions: DataOptions, 
    withSchema: Boolean, 
    strSchema: String, 
    schemaFormat: String, 
    schemaName: String, 
    schemaOptions: SchemaOptions,
    together: Boolean): ValidationResult = {
    println(s"ValidationResult.validate...withSchema: $withSchema, together: $together")

    if (withSchema) {
      Schemas.fromString(strSchema,schemaFormat,schemaName,None) match {
        case Success(schema) => {
          schemaOptions.opt_iri match {
            case Some(iri) => validateIRI(iri, rdf, str_data, dataOptions, schema, strSchema, schemaFormat, schemaOptions,together)
            case None      => validateAny(rdf, str_data, dataOptions, schema, strSchema, schemaFormat, schemaOptions,together)
          }
        }
        case Failure(e) => {
          Logger.info("Schema did not parse..." + e.getMessage)
          ValidationResult(Some(false),
            s"Schema did not parse : ${e.getMessage}, name: $schemaName",
            Result.empty, List(), str_data, dataOptions, true,
            strSchema, schemaFormat, schemaName, schemaOptions,together
            )
        }
      }
    } else if (together) {
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
                   together)
        }
        case Failure(e) =>
          ValidationResult(Some(false),
            s"Schema did not parse : ${e.getMessage}, name: $schemaName",
            Result.empty, List(), str_data, dataOptions, true,
            strSchema, schemaFormat, schemaName, schemaOptions,together
            )
      }
    } else {
      ValidationResult(Some(true), "RDF parsed",
        Result.empty, List(), str_data, dataOptions, false,
        strSchema, schemaFormat, schemaName, schemaOptions,together)
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
    schemaOptions: SchemaOptions,
    together: Boolean): ValidationResult = {
    println(s"ValidationResult.validate_withSchema...withSchema: together: $together")
    val schemaFormat = schema.defaultFormat
    val strSchema = schema.serialize(schemaFormat).getOrElse("")
    schemaOptions.opt_iri match {
            case Some(iri) => validateIRI(iri, rdf, str_data, dataOptions, schema, strSchema, schemaFormat, schemaOptions,together)
            case None      => validateAny(rdf, str_data, dataOptions, schema, strSchema, schemaFormat, schemaOptions,together)
    }
  }

}
