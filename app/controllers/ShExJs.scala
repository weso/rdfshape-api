package controllers

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc.{ Action, AnyContent, Controller }

trait ShExJs{ this: Controller =>

  val endpoint = "http://localhost:4290/validate"
  

  def validate(
      data: String, 
      dataFormat: String,
      schema: String,
      schemaFormat: String): Action[AnyContent] = { 
    Action.async {
    println("Validating shexJs. Data: " + data)
    println("Schema: " + data)
    val postData = Map(
      "data" -> Seq(data),
      "schema" -> Seq(schema)
    )
    
    val postDataJson = Json.toJson(Map(
      "data" -> data,
      "schema" -> schema)
    )
    
    // TODO: It seems that eric's implementation accepts the following
    // curent parameters for the file-upload-only interface are:
    //  schema=<file upload>, start=<relative or prefixed URL>
    //  data=<file upload>, focus=<relative or prefixed URL>
    // Problem...how to specify in Play <file-upload>
    // Postman, it is: "form-data" and has the option FILE
    //          it is not: x-www-form-urlencoded
    
    val responseFuture = WS.url(endpoint).
                         post(data)
    responseFuture.map { response => {
      println("Response body: " + response.body)
      println("Response: " + response)
      Ok(response.body) } 
    }
    }
  }

}

object ShExJs extends Controller with ShExJs