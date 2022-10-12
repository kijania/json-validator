package json.validator.api

import json.validator.domain.model.DomainServiceError
import json.validator.domain.model.DomainServiceError.InvalidRequestError
import org.http4s.{MalformedMessageBodyFailure, Uri}

package object routes {
  implicit class HttpErrorOps(error: Throwable) {
    def toDomainError: DomainServiceError = error match {
      case MalformedMessageBodyFailure(details: String, _) =>
        InvalidRequestError(details)
      case throwable                                       =>
        InvalidRequestError(throwable.getMessage)
    }
  }

  implicit class UriOps(rawUri: String) {
    def toUri: Uri =
      Uri.fromString(rawUri).toOption.get
  }
}
