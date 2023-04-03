#!/usr/bin/env bash

export BASIC_AUTH_CREDENTIALS_PATH=.deploy/demo/helm/config/basic-auth.json

# uncomment to use configuration from demo helm template
# export APP_CONFIG=$(pwd)/app/src/main/resources/application.conf
# export SPRING_CONFIG=$(pwd)/app/src/main/resources/application.yaml

# uncomment to enable debug mode
export EXTRA_OPTS="" # "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=10044"

if [ ! -z "$APP_CONFIG" ]; then
  export APP_CONFIG="-Dconfig.file=$APP_CONFIG"
fi

if [ ! -z "$SPRING_CONFIG" ]; then
  export SPRING_CONFIG="-Dspring.config.location=$SPRING_CONFIG"
fi

java -Dlogging.config=app/src/test/resources/logback-local.xml \
  $APP_CONFIG $SPRING_CONFIG \
  -jar app/build/libs/droidherd-service.jar $EXTRA_OPTS
