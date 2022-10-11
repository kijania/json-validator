package json.validator.api.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import json.validator.domain.model.JsonSchema

case class JsonValidatorResponse(
    action: Action,
    id: JsonSchema.Id,
    status: ActionStatus,
    message: Option[String] = None
)

object JsonValidatorResponse {
  implicit val encoder: Encoder[JsonValidatorResponse] = deriveEncoder[JsonValidatorResponse].mapJson(_.deepDropNullValues)
  implicit val decoder: Decoder[JsonValidatorResponse] = deriveDecoder[JsonValidatorResponse]

  def success(action: Action, id: JsonSchema.Id): JsonValidatorResponse =
    JsonValidatorResponse(action, id, ActionStatus.Success)

  def error(action: Action, id: JsonSchema.Id, message: String): JsonValidatorResponse =
    JsonValidatorResponse(action, id, ActionStatus.Error, Some(message))
}
