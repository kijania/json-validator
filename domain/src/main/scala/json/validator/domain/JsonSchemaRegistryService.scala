package json.validator.domain

import io.circe.JsonObject
import json.validator.domain.model.DomainServiceError.UniquenessViolationError
import json.validator.domain.model.{DomainServiceError, JsonSchema}
import zio.{IO, ULayer, ZIO, ZLayer}

import scala.collection.concurrent.TrieMap

trait JsonSchemaRegistryService {
  def register(schema: JsonSchema): IO[DomainServiceError, Unit]

  def get(id: JsonSchema.Id): IO[DomainServiceError, Option[JsonObject]]
}

object JsonSchemaRegistryService {
  def register(schema: JsonSchema): ZIO[JsonSchemaRegistryService, DomainServiceError, Unit] =
    ZIO.serviceWithZIO[JsonSchemaRegistryService](_.register(schema))

  def get(id: JsonSchema.Id): ZIO[JsonSchemaRegistryService, DomainServiceError, Option[JsonObject]] =
    ZIO.serviceWithZIO[JsonSchemaRegistryService](_.get(id))
}

class InMemoryJsonSchemaRegistryService extends JsonSchemaRegistryService {
  private val schemas: TrieMap[JsonSchema.Id, JsonSchema] = TrieMap.empty

  override def register(schema: JsonSchema): IO[DomainServiceError, Unit] =
    ZIO.cond(
      !schemas.contains(schema.id),
      schemas.put(schema.id, schema),
      UniquenessViolationError(schema.id)
    ).unit

  override def get(id: JsonSchema.Id): IO[DomainServiceError, Option[JsonObject]] =
    ZIO.succeed(schemas.get(id).map(_.schema))
}

object InMemoryJsonSchemaRegistryService {
  def layer: ULayer[JsonSchemaRegistryService] =
    ZLayer.succeed(new InMemoryJsonSchemaRegistryService())
}

