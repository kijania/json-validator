package json.validator.api.model

import enumeratum.EnumEntry.LowerCamelcase
import enumeratum._

sealed trait ActionStatus extends EnumEntry with LowerCamelcase

object ActionStatus extends Enum[ActionStatus] with CirceEnum[ActionStatus] {
  override def values: IndexedSeq[ActionStatus] = findValues

  case object Success extends ActionStatus
  case object Error   extends ActionStatus
}
