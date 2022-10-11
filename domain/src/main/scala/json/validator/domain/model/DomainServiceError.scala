package json.validator.domain.model

sealed trait DomainServiceError {
  def message: String
}

object DomainServiceError {
  case class UniquenessViolationError(id: JsonSchema.Id) extends DomainServiceError {
    override def message: String = s"Schema with id: '$id' already exists"
  }
  case class InvalidRequestError(message: String) extends DomainServiceError
}