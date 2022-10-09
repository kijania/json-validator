package json.validator.api.routes

import io.circe.{Decoder, Encoder, JsonObject}
import json.validator.api.model.{Action, JsonValidatorResponse}
import json.validator.api.routes.JsonSchemaRegistry.Effect
import json.validator.domain.JsonSchemaRegistryService
import json.validator.domain.model.JsonSchema
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import zio.{RIO, URIO, ZIO}
import zio.interop.catz._

object JsonSchemaRegistry {

  type Effect[T] = RIO[JsonSchemaRegistryService, T]
  private val dsl = Http4sDsl[Effect]
  import dsl._

  implicit def circeJsonDecoder[T: Decoder]: EntityDecoder[Effect, T] = jsonOf[Effect, T]

  implicit def circeJsonEncoder[T: Encoder]: EntityEncoder[Effect, T] = jsonEncoderOf[Effect, T]

  val routes: HttpRoutes[Effect] = {
    HttpRoutes.of[Effect] { case request @ POST -> Root / "schema" / schemaId =>
      // TODO catch invalid JSON later to be able to wrap it with Response format
      request.decode[JsonObject](jsonSchema =>
        JsonSchemaRegistryService
          .register(JsonSchema(schemaId, jsonSchema))
          .foldZIO(throwable =>
            ZIO.logError(s"error: ${throwable.getMessage}") *> ZIO.succeed(BadRequest(JsonValidatorResponse.error(Action.UploadSchema, schemaId, throwable.getMessage))),
            _ => ZIO.logInfo("great success") *> Created(JsonValidatorResponse.success(Action.UploadSchema, schemaId))
          ).asInstanceOf[Effect[Response[Effect]]]
      )
    }
  }
}
