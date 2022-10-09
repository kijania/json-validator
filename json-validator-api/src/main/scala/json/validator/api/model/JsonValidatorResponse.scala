package json.validator.api.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class JsonValidatorResponse(
    action: Action,
    id: String,
    status: Status,
    message: Option[String] = None
)

object JsonValidatorResponse {
  implicit val codec: Codec[JsonValidatorResponse] = deriveCodec

  def success(action: Action, id: String): JsonValidatorResponse =
    JsonValidatorResponse(action, id, Status.Success)

  def error(action: Action, id: String, message: String): JsonValidatorResponse =
    JsonValidatorResponse(action, id, Status.Error, Some(message))
}
