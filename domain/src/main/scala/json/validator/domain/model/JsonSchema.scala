package json.validator.domain.model

import io.circe.JsonObject

case class JsonSchema(id: JsonSchema.Id, schema: JsonObject)

object JsonSchema {
  type Id = String
}
