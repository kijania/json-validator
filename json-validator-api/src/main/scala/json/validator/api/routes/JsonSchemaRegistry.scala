package json.validator.api.routes

import io.circe.{Decoder, Encoder, JsonObject}
import json.validator.api.model.{Action, JsonValidatorResponse}
import json.validator.domain.JsonSchemaRegistryService
import json.validator.domain.model.DomainServiceError.{InvalidRequestError, NotFoundError, UniquenessViolationError}
import json.validator.domain.model.JsonSchema
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import zio.RIO
import zio.interop.catz._

object JsonSchemaRegistry {

  type Effect[T] = RIO[JsonSchemaRegistryService, T]
  private val dsl = Http4sDsl[Effect]

  import dsl._

  implicit def circeJsonDecoder[T: Decoder]: EntityDecoder[Effect, T] = jsonOf[Effect, T]

  implicit def circeJsonEncoder[T: Encoder]: EntityEncoder[Effect, T] = jsonEncoderOf[Effect, T]

  val routes: HttpRoutes[Effect] = {
    HttpRoutes.of[Effect] {
      case request @ POST -> Root / "schema" / schemaId =>
        request
          .as[JsonObject]
          .mapError(_.toDomainError)
          .flatMap(jsonSchema => JsonSchemaRegistryService.register(JsonSchema(schemaId, jsonSchema)))
          .foldZIO(
            {
              case InvalidRequestError(message) =>
                BadRequest(JsonValidatorResponse.error(Action.UploadSchema, schemaId, message))
              case er: UniquenessViolationError =>
                UnprocessableEntity(JsonValidatorResponse.error(Action.UploadSchema, schemaId, er.message))
              case er                           =>
                InternalServerError(JsonValidatorResponse.error(Action.UploadSchema, schemaId, er.message))
            },
            _ => Created(JsonValidatorResponse.success(Action.UploadSchema, schemaId))
          )
      case GET -> Root / "schema" / schemaId            =>
        JsonSchemaRegistryService
          .get(schemaId)
          .foldZIO(
            {
              case _: NotFoundError => NotFound()
              case er => InternalServerError(er.message)
            },
            Ok(_)
          )
    }
  }
}
