package json.validator.domain

import io.circe.Json
import io.circe.schema.Schema
import json.validator.domain.model.DomainServiceError
import json.validator.domain.model.DomainServiceError.ValidationError
import zio.{IO, ULayer, ZIO, ZLayer}

trait JsonValidationService {
  def validate(json: Json, schema: Json): IO[DomainServiceError, Unit]
}

object JsonValidationService {
  def validate(json: Json, schema: Json): ZIO[JsonValidationService, DomainServiceError, Unit] =
    ZIO.serviceWithZIO[JsonValidationService](_.validate(json, schema))
}

class CirceJsonSchemaValidator extends JsonValidationService {
  override def validate(json: Json, schema: Json): IO[DomainServiceError, Unit] =
    ZIO.fromEither(
      Schema
        .load(schema)
        .validate(json)
        .leftMap(errors =>
          ValidationError(
            errors.map(_.getMessage)
          )
        )
        .toEither
    )
}

object CirceJsonSchemaValidator {
  def layer: ULayer[JsonValidationService] =
    ZLayer.succeed(new CirceJsonSchemaValidator())
}
