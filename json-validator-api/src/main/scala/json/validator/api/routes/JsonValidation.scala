package json.validator.api.routes

import io.circe.syntax._
import io.circe.{Decoder, Encoder, JsonObject}
import json.validator.api.model.{Action, JsonValidatorResponse}
import json.validator.domain.model.DomainServiceError.{NotFoundError, ValidationError}
import json.validator.domain.{JsonSchemaRegistryService, JsonValidationService}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import zio.interop.catz._
import zio.{RIO, ZIO}

object JsonValidation {

  type Effect[T] = RIO[JsonSchemaRegistryService with JsonValidationService, T]
  private val dsl = Http4sDsl[Effect]

  import dsl._

  implicit def circeJsonDecoder[T: Decoder]: EntityDecoder[Effect, T] = jsonOf[Effect, T]

  implicit def circeJsonEncoder[T: Encoder]: EntityEncoder[Effect, T] = jsonEncoderOf[Effect, T]

  val routes: HttpRoutes[Effect] = {
    HttpRoutes.of[Effect] { case request @ POST -> Root / "validate" / schemaId =>
      (for {
        json       <- request
                        .as[JsonObject]
                        .map(_.asJson.deepDropNullValues)
                        .mapError(_.toDomainError)
        jsonSchema <- JsonSchemaRegistryService.get(schemaId).map(_.asJson)
        _          <- JsonValidationService.validate(json, jsonSchema)
        // TODO refactor error handling to one mapper or extract it from the domain service error
      } yield JsonValidatorResponse.success(Action.ValidateDocument, schemaId)).foldZIO(
        {
          case er: ValidationError =>
            UnprocessableEntity(JsonValidatorResponse.error(Action.ValidateDocument, schemaId, er.message))
          case er: NotFoundError   => NotFound(JsonValidatorResponse.error(Action.ValidateDocument, schemaId, er.message))
          case er                  => InternalServerError(JsonValidatorResponse.error(Action.ValidateDocument, schemaId, er.message))
        },
        Ok(_)
      )
    }
  }
}
