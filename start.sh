#!/bin/bash

sbt clean docker
java -jar json-validator-api/target/scala-2.13/json-validator-api.jar