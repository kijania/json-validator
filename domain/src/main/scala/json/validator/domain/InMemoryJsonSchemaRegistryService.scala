package json.validator.domain

import io.circe.JsonObject
import json.validator.domain.model.DomainServiceError.{NotFoundError, UniquenessViolationError}
import json.validator.domain.model.{DomainServiceError, JsonSchema}
import zio.{IO, ULayer, ZIO, ZLayer}

import scala.collection.concurrent.TrieMap

class InMemoryJsonSchemaRegistryService extends JsonSchemaRegistryService {
  private val schemas: TrieMap[JsonSchema.Id, JsonSchema] = TrieMap.empty

  override def register(schema: JsonSchema): IO[DomainServiceError, Unit] =
    ZIO.cond(
      !schemas.contains(schema.id),
      schemas.put(schema.id, schema),
      UniquenessViolationError(schema.id)
    ).unit

  override def get(id: JsonSchema.Id): IO[DomainServiceError, JsonObject] =
    ZIO.fromEither(
      schemas
        .get(id)
        .map(_.schema)
        .toRight(NotFoundError(id))
    )
}

object InMemoryJsonSchemaRegistryService {
  def layer: ULayer[JsonSchemaRegistryService] =
    ZLayer.succeed(new InMemoryJsonSchemaRegistryService())
}
