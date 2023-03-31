# Droidherd
DroidHerd service — k8s android farm orchestration service.

## What is it
DroidHerd is a service which manages android farm in k8s and provides single interface to clients to use it with support
of the following functionality:
- authorization
- multiple sessions
- quotas for clients
- API to be able to customize running emulators for each client

## How it works
Service contains 2 main components: rest controller to serve API requests from clients (and admin) and operator.

Clients make requests to the service using API to get the required number of android emulators having specified versions.

Service works as a k8s operator: each session request [transforms to the CRD](crd-model/src/main/crd/testops.tinkoff.ru_droidherdsessions.yaml) (Custom Resource Definition) and then service works with CRD to reach desired state (run required emulators).

Service is cloud-native and supports multiple replicas. Also, it supports quota for clients and sessions, so clients can create multiple sessions — service
will monitor quota usage and restrict quota exceeding.

### Namespaced operator

Service designed to be used in isolated namespace.

So if you want to run another one service in parallel you just need to deploy it
to separate NS and configure to use.

But CRD is not namespaced - they are global. If you want to change it - you must create new version of resource instead
change existing to avoid conflicts with runtime CRD. It is not service requirement - it is design rule for any CRD in k8s. 

### Android emulator readiness

Readiness of emulator is important part of service because if emulator failed to start even if adb allow to connect to it
any communication with them is undefined and at mostly cases leads to errors on client.

But service doesn't cover logic to check that emulator is ready.

By service design it should be done by k8s probes which can be configured in pod template yaml in configuration.

[Example](.deploy/demo/helm/resources/emulator-startup-probe.sh) of readiness script provided with demo helm template.

## Clients API

See the Swagger documentation at `/swagger-ui.html`.

### Java API

Java API models can be found in [api module](api/src/main/java/ru/tinkoff/testops/droidherd/api/SessionRequest.java)

For java clients API is published to public maven oss repository and you add dependency to your build:
```text
ru.tinkoff.testops.droidherd:api:1.0.0
```

Example of using API you can find in [droidherd-fork](https://github.com/tinkoff-mobile-tech/fork) test runner.

## Configuration

Main application config is stored [here](app/src/main/resources/application.conf).

Description for all parameters available there.

## Security

In order to demonstrate how Droidherd works we have implemented basic auth scheme. For simplicity, all clients'
credentials are stored in [credentials.json](.deploy/demo/helm/config/basic-auth.json).

Client 'droidherd-default' is configured there without password by default.

### Customization

You can provide own implementation of security by implementing auto-configuration and extending
AuthService interface. Check out basic-auth module as example.

## How to run

First thing you need to run the service — kubernetes cluster. Without it Droidherd service will fail at startup during 
the dry run stage. 

You can disable dry run (see [Configuration](#configuration)), **but we highly recommend not to do this**.

Alternative way to run service locally without android emulators (described below).

### Demo helm template

Demo helm template with all required configuration placed in [demo](.deploy/demo/helm/Chart.yaml) folder.

[README](.deploy/demo/helm/README.md) contains additional details how to install it.

Be aware that android emulators itself are not a part of the service — you can use anyone 
which you found or developed by yourself.

For demo purposes service use [android images](https://github.com/google/android-emulator-container-scripts) developed by google.

### Local run

By default embedded configuration in classpath contains echo-server instead android emulator images.

In mostly cases if you want to test and debug service - you don't need android images which is require a lot of resources.

In repository you can find [local-run-starter.sh](local-run-starter.sh) script to run service from command line.
This script use configuration from demo help template. Override it in script if required.

Also, we provide intellij idea run configuration (inside .run folder).


### Pitfalls

Don't forget that if you run service locally you will be use configuration from local files.

Once service running in k8s will be use configuration from configmaps.

Also, don't forget to scale replicas to 0 in k8s to be sure that your local
running service will be active. 

**Do not forget to specify your namespace and android images** before running (see [Configuration](#configuration)).

### Replicas

Service designed to be able running in multiple replicas.

There currently no leader election and operator can duplicate request to k8s (create pod/service).
But all requests designed to be idempotent so there no harm do to it more than one time.

### Keep alive

Last seen attribute stored and updated in CRD resource.

Using this attribute service take decision that session is stuck and release it automatically.

Clients must use 'pinger' to send keep alive for their sessions.

Stuck session can be caused by killing jobs on CI so it is important part of service to avoid stuck sessions.

## Emulators customization

Service designed to work with any emulator image. But we need to way to configure it somehow. 

You can tune template for pod/service and enrich them with required attributes.
For example, we use [envoyproxy](https://www.envoyproxy.io/) as sidecar for emulator in our corporate pod template to manage
emulator traffic.

Service template can be extended to provide additional ports which you want to open from pod.
Droidherd API provides in session status extraUris attributes which is map all extra service ports by their name. 

To pass parameters from client use 'emulatorParameters' attribute
(details you can find in [api module](api/src/main/java/ru/tinkoff/testops/droidherd/api/Emulator.java)).

Each parameter mapped to 'EMULATOR_${KEY}' = value and pass as is to pod template.

Then you can use [jinja](https://github.com/HubSpot/jinjava) in yml templates with some trivial logic:
```yaml
# client pass parameter: with_emulated_camera=true
- name: emulator
  env:
    {% if EMULATOR_WITH_EMULATED_CAMERA == 'true' %}
    - name: EMULATOR_PARAMS
      value: '-camera-back emulated -camera-front emulated'
    {% endif %}
  # passthrough as is to env variable
    - name: MY_ENV_VAR_WITH_EMULATED_CAMERA
      value: {{EMULATOR_WITH_EMULATED_CAMERA}}
```

Note: By default parameters not passed to env variables to pod. You must add them explicitly.

Once you got env variable - you can use it inside your emulator startup script and implement required logic.

## Contributing

See our [CONTRIBUTING.md](CONTRIBUTING.md) guide.

## License

🆓 Feel free to use our service in your commercial and private applications.

All DroidHerd components are covered by [Apache 2.0](LICENSE)

Read more about this license [here](https://choosealicense.com/licenses/apache-2.0/).
