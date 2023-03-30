#!/usr/bin/env bash

export APP_CONFIG=$(pwd)/app/src/main/resources/application.conf
export SPRING_CONFIG=$(pwd)/app/src/main/resources/application.yaml
export BASIC_AUTH_CREDENTIALS_PATH=.deploy/demo/helm/config/basic-auth.json

# uncomment to enable debug mode
export EXTRA_OPTS="" # "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=10044"

java -Dlogging.config=app/src/test/resources/logback-local.xml \
  -Dconfig.file=$APP_CONFIG -Dspring.config.location=$SPRING_CONFIG \
  -jar app/build/libs/droidherd-service.jar $EXTRA_OPTS
