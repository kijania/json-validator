# Json Validator

### Run application:

1. Run `./start.sh`

it would:
* compile application
* build Docker image
* and run application

### Run tests, format code and build docker image:

1. Run `./ci.sh`

### Implementation details:
Application is using `circe-json-schema` for schema validation, it provides errors accumulation

More generic resource management on connection between Http4s and ZIO would be beneficial

Extracted HTTP Error Mapper would be beneficial.

Endpoint GET /schema/SCHEMAID is providing response not in the same format as other two endpoints, because it wasn't requested, and it looked it might be designed for different audience, e.g. internal users 