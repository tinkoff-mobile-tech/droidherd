#!/usr/bin/env bash

# helper script to build and push docker image

DOCKER_IMAGE_PREFIX=${DOCKER_IMAGE_PREFIX:-"localhost:5000/"}
DOCKER_TAG=${DOCKER_TAG:-"local"}
DOCKER_IMAGE=${DOCKER_IMAGE:-"droidherd-service"}

echo "Building docker image: ${DOCKER_IMAGE_PREFIX}${DOCKER_IMAGE}:${DOCKER_TAG} ..."
docker build -f OssDockerfile -t ${DOCKER_IMAGE_PREFIX}${DOCKER_IMAGE}:${DOCKER_TAG} .
echo "Pushing image ..."
docker push ${DOCKER_IMAGE_PREFIX}${DOCKER_IMAGE}:${DOCKER_TAG}
