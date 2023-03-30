#!/usr/bin/env bash

# this is helper script to quick install helm template

# register droidherd CRD in k8s cluster
kubectl apply -f ../../../crd-model/src/main/crd/testops.tinkoff.ru_droidherdsessions.yaml

# deploy service
helm -n droidherd-service upgrade --install droidherd-service .
