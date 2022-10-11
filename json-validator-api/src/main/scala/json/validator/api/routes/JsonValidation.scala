package json.validator.api.routes

import io.circe.schema.Schema
import io.circe.syntax._
import io.circe.{Decoder, Encoder, JsonObject}
import json.validator.api.model.{Action, JsonValidatorResponse}
import json.validator.domain.JsonSchemaRegistryService
import json.validator.domain.model.DomainServiceError.{InvalidRequestError, NotFoundError, ValidationError}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, MalformedMessageBodyFailure}
import zio.interop.catz._
import zio.{RIO, ZIO}

object JsonValidation {

  type Effect[T] = RIO[JsonSchemaRegistryService, T]
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
                        .mapError {
                          case MalformedMessageBodyFailure(details: String, _) =>
                            InvalidRequestError(details)
                          case throwable                                       =>
                            InvalidRequestError(throwable.getMessage)
                        }
        // TODO generalize this handling to above method
        jsonSchema <- JsonSchemaRegistryService
                        .get(schemaId)
                        .flatMap(ZIO.fromOption(_).mapError(_ => NotFoundError(schemaId)))
        _          <-
          // TODO refactor this way to enable changing schema validation function
          ZIO.fromEither(
            Schema
              .load(jsonSchema.asJson)
              .validate(json)
              .leftMap(errors =>
                ValidationError(
                  errors.map(_.getMessage)
                )
              )
              .toEither
          )
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
