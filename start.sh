#!/bin/bash

sbt clean test fmt docker
java -jar json-validator-api/target/scala-2.13/json-validator-api.jar