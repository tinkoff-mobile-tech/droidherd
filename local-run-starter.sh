#!/usr/bin/env bash

java -Dlogging.config=app/src/test/resources/logback-local.xml \
  -Dloader.path=lib -jar app/build/libs/droidherd-service.jar \
#  -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=10044 \

