package json.validator.api.routes

import io.circe.{Decoder, Encoder, JsonObject}
import json.validator.api.model.{Action, JsonValidatorResponse}
import json.validator.domain.JsonSchemaRegistryService
import json.validator.domain.model.JsonSchema
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, MalformedMessageBodyFailure}
import zio.interop.catz._
import zio.{RIO, ZIO}

object JsonSchemaRegistry {

  type Effect[T] = RIO[JsonSchemaRegistryService, T]
  private val dsl = Http4sDsl[Effect]

  import dsl._

  implicit def circeJsonDecoder[T: Decoder]: EntityDecoder[Effect, T] = jsonOf[Effect, T]

  implicit def circeJsonEncoder[T: Encoder]: EntityEncoder[Effect, T] = jsonEncoderOf[Effect, T]

  val routes: HttpRoutes[Effect] = {
    HttpRoutes.of[Effect] { case request @ POST -> Root / "schema" / schemaId =>
      request
        .as[JsonObject]
        .flatMap(jsonSchema => JsonSchemaRegistryService.register(JsonSchema(schemaId, jsonSchema)))
        .foldZIO(
          {
            case MalformedMessageBodyFailure(details: String, _) =>
              BadRequest(JsonValidatorResponse.error(Action.UploadSchema, schemaId, details))
            case throwable                                       =>
              InternalServerError(JsonValidatorResponse.error(Action.UploadSchema, schemaId, throwable.getMessage))
          },
          _ => ZIO.logInfo("great success") *> Created(JsonValidatorResponse.success(Action.UploadSchema, schemaId))
        )
    }
  }
}
