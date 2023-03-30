# Demo helm template

This is template designed for demo purposes.

## Overview

Template contains necessary manifests to deploy droidherd-service itself and minimal required configuration
to run android emulators.

All configuration deployed as configmaps so you can change it in k8s directly for any
testing. But droidherd-service currently cannot refresh configuration on fly so don't forget to restart it.

## How to apply

Use local-install.sh script as template and configure required values in values.yaml.

Before install also need to apply CRD for cluster, it is placed crd-model:

```bash
kubectl apply -f crd-model/src/main/crd/testops.tinkoff.ru_droidherdsessions.yaml
```

## How to use

Swagger:

By default basic-auth provided with several test clients.
They are placed in config/basic-auth.json file.

Client id 'droidherd-default' available to use without password.

## Docker android images

Use images from [google](https://github.com/google/android-emulator-container-scripts) which is public available.

Be aware that some configuration for them added to pod-emulator.yml template which can be significantly differ
if you will be run other android images.

Also, special script emulator-startup-probe is mounted to emulator and used
to check that emulator is ready. It is important part because
droidherd-service use 'ready' attribute to determine that emulator can be accessible by client.

### Android images are 'heavy'

Be aware that android images require a lot of space (especially created by google) and during initial run on k8s node
it may cause a lot of traffic in your network.

Recommended way to pre-pull required images to your k8s nodes manually 
before open public access to service.


# Troubleshooting

## Dry run failed - k8s fails with 404 error

It its caused because CRD is not installed.

Install CRD which is placed in crd-model to your cluster.
```bash
kubectl apply -f crd-model/src/main/crd/testops.tinkoff.ru_droidherdsessions.yaml
```
