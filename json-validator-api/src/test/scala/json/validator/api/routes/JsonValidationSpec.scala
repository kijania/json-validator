package json.validator.api.routes

import io.circe.Json
import io.circe.parser.decode
import json.validator.api.model.JsonValidatorResponse._
import json.validator.api.model.{Action, ActionStatus, JsonValidatorResponse}
import json.validator.domain.{CirceJsonSchemaValidator, InMemoryJsonSchemaRegistryService}
import org.http4s.{Method, Request, Status}
import org.scalatest.EitherValues._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._

import scala.io.Source

object JsonValidationSpec extends ZIOSpecDefault {

  type UploadResponse[T]     = JsonSchemaRegistry.Effect[T]
  type ValidationResponse[T] = JsonValidation.Effect[T]
  private val schemaRegistryApp = JsonSchemaRegistry.routes.orNotFound
  private val validationApp     = JsonValidation.routes.orNotFound

  private val testLayer = InMemoryJsonSchemaRegistryService.layer ++ CirceJsonSchemaValidator.layer

  override def spec = {
    suite("JsonValidation")(
      test("should successfully validate json against json schema") {
        val jsonSchema = decode[Json](Source.fromResource("config-schema.json").mkString).value
        val json       = decode[Json](Source.fromResource("config.json").mkString).value

        val uploadSchemaRequest = Request[UploadResponse](Method.POST, "/schema/config-schema".toUri).withEntity(jsonSchema)(
          JsonSchemaRegistry.circeJsonEncoder
        )
        val validateJsonRequest = Request[ValidationResponse](Method.POST, "/validate/config-schema".toUri).withEntity(json)(
          JsonValidation.circeJsonEncoder
        )

        val expectedValidationResponse = JsonValidatorResponse(Action.ValidateDocument, "config-schema", ActionStatus.Success)
        (
          for {
            _                  <- schemaRegistryApp.run(uploadSchemaRequest)
            validationResponse <- validationApp.run(validateJsonRequest)
            body               <- validationResponse.as[JsonValidatorResponse](asyncInstance, JsonValidation.circeJsonDecoder)
          } yield assert(validationResponse.status)(equalTo(Status.Ok)) &&
            assert(body)(equalTo(expectedValidationResponse))
        ).provideLayer(testLayer)
      },
      test("should return errors for invalid json against json schema") {
        val jsonSchema       = decode[Json](Source.fromResource("config-schema.json").mkString).value
        val incompatibleJson = decode[Json](Source.fromResource("config.json").mkString.replace("1024", "null")).value

        val uploadSchemaRequest = Request[UploadResponse](Method.POST, "/schema/config-schema".toUri).withEntity(jsonSchema)(
          JsonSchemaRegistry.circeJsonEncoder
        )
        val invalidJsonRequest  = Request[ValidationResponse](Method.POST, "/validate/config-schema".toUri).withEntity(
          incompatibleJson
        )(JsonValidation.circeJsonEncoder)

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
            body               <- validationResponse.as[JsonValidatorResponse](asyncInstance, JsonValidation.circeJsonDecoder)
          } yield assert(validationResponse.status)(equalTo(Status.UnprocessableEntity)) &&
            assert(body)(equalTo(expectedValidationResponse))
        ).provideLayer(testLayer)
      },
      test("should return not found when attempting to validate json against not existent schema") {
        val json                = decode[Json](Source.fromResource("config.json").mkString).value
        val validateJsonRequest = Request[ValidationResponse](Method.POST, "/validate/config-schema".toUri).withEntity(json)(
          JsonValidation.circeJsonEncoder
        )

        val expectedValidationResponse = JsonValidatorResponse(
          Action.ValidateDocument,
          "config-schema",
          ActionStatus.Error,
          Some("Schema with id: 'config-schema' could not be found'")
        )
        (
          for {
            validationResponse <- validationApp.run(validateJsonRequest)
            body               <- validationResponse.as[JsonValidatorResponse](asyncInstance, JsonValidation.circeJsonDecoder)
          } yield assert(validationResponse.status)(equalTo(Status.NotFound)) &&
            assert(body)(equalTo(expectedValidationResponse))
        ).provideLayer(testLayer)
      }
    )
  }
}
