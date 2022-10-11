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

object JsonSchemaRegistrySpec extends ZIOSpecDefault {

  type Response[T] = JsonSchemaRegistry.Effect[T]
  private val app = JsonSchemaRegistry.routes.orNotFound

  override def spec: Spec[Any, Throwable] =
    suite("JsonSchemaRegistry")(
      test("should register new schema") {
        val jsonSchema = decode[Json](Source.fromResource("config-schema.json").mkString).value
        val request    = Request[Response](Method.POST, "/schema/config-schema".toUri).withEntity(jsonSchema)

        val expected = JsonValidatorResponse(Action.UploadSchema, "config-schema", ActionStatus.Success)
        (
          for {
            response <- app.run(request)
            body     <- response.as[JsonValidatorResponse]
          } yield assert(response.status)(equalTo(Status.Created)) &&
            assert(body)(equalTo(expected))
        ).provideLayer(InMemoryJsonSchemaRegistryService.layer)
      },
      test("should fail to register schema with non unique name") {
        val jsonSchema = decode[Json](Source.fromResource("config-schema.json").mkString).value
        val request    = Request[Response](Method.POST, "/schema/config-schema".toUri).withEntity(jsonSchema)

        val expected = JsonValidatorResponse(
          Action.UploadSchema,
          "config-schema",
          ActionStatus.Error,
          Some("Schema with id: 'config-schema' already exists")
        )
        (
          for {
            _                    <- app.run(request)
            duplicatedIdResponse <- app.run(request)
            body                 <- duplicatedIdResponse.as[JsonValidatorResponse]
          } yield assert(duplicatedIdResponse.status)(equalTo(Status.UnprocessableEntity)) &&
            assert(body)(equalTo(expected))
        ).provideLayer(InMemoryJsonSchemaRegistryService.layer)
      },
      test("should fail to register malformed schema") {
        val request = Request[Response](Method.POST, "/schema/config-schema".toUri).withEntity("It's not a valid JSON")
        (
          for {
            invalidJsonResponse <- app.run(request)
          } yield assert(invalidJsonResponse.status)(equalTo(Status.BadRequest))
        ).provideLayer(InMemoryJsonSchemaRegistryService.layer)
      },
      test("should return registered schema") {
        val jsonSchema      = decode[Json](Source.fromResource("config-schema.json").mkString).value
        val registerRequest = Request[Response](Method.POST, "/schema/config-schema".toUri).withEntity(jsonSchema)
        val getRequest      = Request[Response](Method.GET, "/schema/config-schema".toUri)

        (
          for {
            _           <- app.run(registerRequest)
            getResponse <- app.run(getRequest)
            body        <- getResponse.as[Json]
          } yield assert(getResponse.status)(equalTo(Status.Ok)) &&
            assert(body)(equalTo(jsonSchema))
        ).provideLayer(InMemoryJsonSchemaRegistryService.layer)
      },
      test("should return not found when getting not existent schema") {
        val getRequest = Request[Response](Method.GET, "/schema/config-schema".toUri)
        app
          .run(getRequest)
          .map(response => assert(response.status)(equalTo(Status.NotFound)))
          .provideLayer(InMemoryJsonSchemaRegistryService.layer)
      }
    )
}
