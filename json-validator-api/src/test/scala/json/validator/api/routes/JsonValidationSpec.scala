package json.validator.api.routes

import io.circe.Json
import io.circe.parser.decode
import json.validator.api.model.JsonValidatorResponse._
import json.validator.api.model.{Action, ActionStatus, JsonValidatorResponse}
import json.validator.api.routes.JsonSchemaRegistry._
import json.validator.domain.InMemoryJsonSchemaRegistryService
import org.http4s.{Method, Request, Status}
import org.scalatest.EitherValues._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._

import scala.io.Source

object JsonValidationSpec extends ZIOSpecDefault {

  type Response[T] = JsonValidation.Effect[T]
  private val schemaRegistryApp = JsonSchemaRegistry.routes.orNotFound
  private val validationApp     = JsonValidation.routes.orNotFound

  override def spec = {
    suite("JsonValidation")(
      test("should successfully validate json against json schema") {
        val jsonSchema = decode[Json](Source.fromResource("config-schema.json").mkString).value
        val json       = decode[Json](Source.fromResource("config.json").mkString).value

        val uploadSchemaRequest = Request[Response](Method.POST, "/schema/config-schema".toUri).withEntity(jsonSchema)
        val validateJsonRequest = Request[Response](Method.POST, "/validate/config-schema".toUri).withEntity(json)

        val expectedValidationResponse = JsonValidatorResponse(Action.ValidateDocument, "config-schema", ActionStatus.Success)
        (
          for {
            _                  <- schemaRegistryApp.run(uploadSchemaRequest)
            validationResponse <- validationApp.run(validateJsonRequest)
            body               <- validationResponse.as[JsonValidatorResponse]
          } yield assert(validationResponse.status)(equalTo(Status.Ok)) &&
            assert(body)(equalTo(expectedValidationResponse))
        ).provideLayer(InMemoryJsonSchemaRegistryService.layer)
      },
      test("should return errors for invalid json against json schema") {
        val jsonSchema       = decode[Json](Source.fromResource("config-schema.json").mkString).value
        val incompatibleJson = decode[Json](Source.fromResource("config.json").mkString.replace("1024", "null")).value

        val uploadSchemaRequest = Request[Response](Method.POST, "/schema/config-schema".toUri).withEntity(jsonSchema)
        val invalidJsonRequest  = Request[Response](Method.POST, "/validate/config-schema".toUri).withEntity(incompatibleJson)

        val expectedValidationResponse = JsonValidatorResponse(
          Action.ValidateDocument,
          "config-schema",
          ActionStatus.Error,
          Some("#/chunks: required key [size] not found")
        )
        (
          for {
            _                  <- schemaRegistryApp.run(uploadSchemaRequest)
            validationResponse <- validationApp.run(invalidJsonRequest)
            body               <- validationResponse.as[JsonValidatorResponse]
          } yield assert(validationResponse.status)(equalTo(Status.UnprocessableEntity)) &&
            assert(body)(equalTo(expectedValidationResponse))
        ).provideLayer(InMemoryJsonSchemaRegistryService.layer)
      },
      test("should return not found when attempting to validate json against not existent schema") {
        val json                = decode[Json](Source.fromResource("config.json").mkString).value
        val validateJsonRequest = Request[Response](Method.POST, "/validate/config-schema".toUri).withEntity(json)

        val expectedValidationResponse = JsonValidatorResponse(
          Action.ValidateDocument,
          "config-schema",
          ActionStatus.Error,
          Some("Schema with id: 'config-schema' could not be found'")
        )
        (
          for {
            validationResponse <- validationApp.run(validateJsonRequest)
            body               <- validationResponse.as[JsonValidatorResponse]
          } yield assert(validationResponse.status)(equalTo(Status.NotFound)) &&
            assert(body)(equalTo(expectedValidationResponse))
        ).provideLayer(InMemoryJsonSchemaRegistryService.layer)
      }
    )
  }
}
