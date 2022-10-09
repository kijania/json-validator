package json.validator.domain.model

import io.circe.JsonObject

case class JsonSchema(id: String, schema: JsonObject)
