package json.validator.domain

import json.validator.domain.model.JsonSchema
import zio.{RIO, Task, UIO, ULayer, ZIO, ZLayer}

trait JsonSchemaRegistryService {
  def register(schema: JsonSchema): Task[Unit]
}

object JsonSchemaRegistryService {
  def register(schema: JsonSchema): RIO[JsonSchemaRegistryService, Unit] =
    ZIO.serviceWithZIO[JsonSchemaRegistryService](_.register(schema))
}

class NaiveJsonSchemaRegistryService extends JsonSchemaRegistryService {
  override def register(schema: JsonSchema): UIO[Unit] =
    ZIO.logInfo(s"Registered new schema: $schema")
}

object NaiveJsonSchemaRegistryService {
  def layer: ULayer[JsonSchemaRegistryService] =
    ZLayer.succeed(new NaiveJsonSchemaRegistryService())
}

