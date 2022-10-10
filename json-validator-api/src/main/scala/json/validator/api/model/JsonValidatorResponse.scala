package json.validator.api.model

import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import json.validator.domain.model.JsonSchema

case class JsonValidatorResponse(
    action: Action,
    id: JsonSchema.Id,
    status: Status,
    message: Option[String] = None
)

object JsonValidatorResponse {
  implicit val encoder: Encoder[JsonValidatorResponse] = deriveEncoder[JsonValidatorResponse].mapJson(_.deepDropNullValues)
  implicit val codec: Decoder[JsonValidatorResponse]   = deriveDecoder[JsonValidatorResponse]

  def success(action: Action, id: JsonSchema.Id): JsonValidatorResponse =
    JsonValidatorResponse(action, id, Status.Success)

  def error(action: Action, id: JsonSchema.Id, message: String): JsonValidatorResponse =
    JsonValidatorResponse(action, id, Status.Error, Some(message))
}
