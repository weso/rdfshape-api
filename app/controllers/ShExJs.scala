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