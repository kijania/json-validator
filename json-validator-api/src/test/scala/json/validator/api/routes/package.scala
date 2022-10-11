package json.validator.api

import org.http4s.Uri

package object routes {
  implicit class UriOps(rawUri: String) {
    def toUri: Uri =
      Uri.fromString(rawUri).toOption.get
  }
}
