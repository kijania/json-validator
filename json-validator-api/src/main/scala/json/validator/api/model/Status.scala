package json.validator.api.model

import enumeratum.EnumEntry.LowerCamelcase
import enumeratum._

sealed trait Status extends EnumEntry with LowerCamelcase

object Status extends Enum[Status] with CirceEnum[Status] {
  override def values: IndexedSeq[Status] = findValues

  case object Success extends Status
  case object Error   extends Status
}
