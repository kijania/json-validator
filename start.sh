#!/bin/bash

sbt clean compile docker
java -jar json-validator-api/target/scala-2.13/json-validator-api.jar