package json.validator.api.model

import enumeratum._

sealed trait Status extends EnumEntry

object Status extends Enum[Status] with CirceEnum[Status] {
  override def values: IndexedSeq[Status] = findValues

  case object Success extends Status
  case object Error   extends Status
}
