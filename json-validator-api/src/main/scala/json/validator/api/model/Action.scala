package json.validator.api.model

import enumeratum._

sealed trait Action extends EnumEntry

object Action extends Enum[Action] with CirceEnum[Action] {
  override def values: IndexedSeq[Action] = findValues

  case object UploadSchema     extends Action
  case object ValidateDocument extends Action
}
