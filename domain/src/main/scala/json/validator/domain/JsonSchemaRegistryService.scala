package json.validator.domain

import json.validator.domain.model.DomainServiceError.UniquenessViolationError
import json.validator.domain.model.{DomainServiceError, JsonSchema}
import zio.{IO, ULayer, ZIO, ZLayer}

import scala.collection.concurrent.TrieMap

trait JsonSchemaRegistryService {
  def register(schema: JsonSchema): IO[DomainServiceError, Unit]
}

object JsonSchemaRegistryService {
  def register(schema: JsonSchema): ZIO[JsonSchemaRegistryService, DomainServiceError, Unit] =
    ZIO.serviceWithZIO[JsonSchemaRegistryService](_.register(schema))
}

class InMemoryJsonSchemaRegistryService extends JsonSchemaRegistryService {
  private val schemas: TrieMap[JsonSchema.Id, JsonSchema] = TrieMap.empty

  override def register(schema: JsonSchema): IO[DomainServiceError, Unit] =
    ZIO.cond(
      !schemas.contains(schema.id),
      schemas.put(schema.id, schema),
      UniquenessViolationError(schema.id)
    ).unit
}

object InMemoryJsonSchemaRegistryService {
  def layer: ULayer[JsonSchemaRegistryService] =
    ZLayer.succeed(new InMemoryJsonSchemaRegistryService())
}

