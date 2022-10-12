package json.validator.api.routes

import io.circe.{Decoder, Encoder, JsonObject}
import json.validator.api.model.{Action, JsonValidatorResponse}
import json.validator.domain.JsonSchemaRegistryService
import json.validator.domain.model.DomainServiceError.{
  InvalidRequestError,
  NotFoundError,
  UniquenessViolationError,
  ValidationError
}
import json.validator.domain.model.{DomainServiceError, JsonSchema}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Response}
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
            errorMapper(Action.UploadSchema, schemaId),
            _ => Created(JsonValidatorResponse.success(Action.UploadSchema, schemaId))
          )
      case GET -> Root / "schema" / schemaId            =>
        JsonSchemaRegistryService
          .get(schemaId)
          .foldZIO(
            {
              case _: NotFoundError => NotFound()
              case er               => InternalServerError(er.message)
            },
            Ok(_)
          )
    }
  }

  private def errorMapper(action: Action, schemaId: JsonSchema.Id): DomainServiceError => Effect[Response[Effect]] = er => {
    val response = JsonValidatorResponse.error(action, schemaId, er.message)
    er match {
      case _: InvalidRequestError      => BadRequest(response)
      case _: NotFoundError            => NotFound(response)
      case _: ValidationError          => UnprocessableEntity(response)
      case _: UniquenessViolationError => UnprocessableEntity(response)
      case _                           => InternalServerError(response)
    }
  }
}
