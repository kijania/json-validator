package json.validator.domain

import io.circe.JsonObject
import json.validator.domain.model.{DomainServiceError, JsonSchema}
import zio.{IO, ZIO}

trait JsonSchemaRegistryService {
  def register(schema: JsonSchema): IO[DomainServiceError, Unit]

  def get(id: JsonSchema.Id): IO[DomainServiceError, JsonObject]
}

object JsonSchemaRegistryService {
  def register(schema: JsonSchema): ZIO[JsonSchemaRegistryService, DomainServiceError, Unit] =
    ZIO.serviceWithZIO[JsonSchemaRegistryService](_.register(schema))

  def get(id: JsonSchema.Id): ZIO[JsonSchemaRegistryService, DomainServiceError, JsonObject] =
    ZIO.serviceWithZIO[JsonSchemaRegistryService](_.get(id))
}
