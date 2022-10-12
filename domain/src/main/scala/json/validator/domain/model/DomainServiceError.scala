package json.validator.domain.model

import cats.data.NonEmptyList

sealed trait DomainServiceError {
  def message: String
}

object DomainServiceError {
  case class UniquenessViolationError(id: JsonSchema.Id) extends DomainServiceError {
    override def message: String = s"Schema with id: '$id' already exists"
  }
  case class InvalidRequestError(message: String) extends DomainServiceError

  case class ValidationError(errors: NonEmptyList[String]) extends DomainServiceError {
    override def message: String = errors.toList.mkString(", ")
  }

  case class NotFoundError(schemaId: JsonSchema.Id) extends DomainServiceError {
    override def message: String = s"Schema with id: '$schemaId' could not be found'"
  }

  case class InternalServerError(message: String) extends DomainServiceError
}