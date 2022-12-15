# Droidherd
DroidHerd service — k8s android farm orchestration service.

## What is it
DroidHerd is a service which manages android farm in k8s and provides single interface to clients to use it with support
of the following functionality:
- authorization
- multiple sessions
- quotas for clients
- API to be able to customize running emulators for each client

Android emulators itself are not a part of the service — you can use any ones which you found or developed by yourself.

## How it works
Service contains 2 main components: rest controller to serve API requests from clients (and admin) and operator.

Clients make requests to the service using API to get the required number of android emulators having specified versions.

Service works as a k8s operator: each request transforms to the CRD (Custom Resource Definition) and then service works with CRD to reach desired state (run required emulators).

Service is cloud-native and supports multiple replicas. Also, it supports quota for clients and sessions, so clients can create multiple sessions — service
will monitor quota usage and restrict quota exceeding.

## Clients API

See the Swagger documentation at `/swagger-ui.html`.

## Configuration

Main application config is stored [here](app/src/main/resources/application.conf). There you can find:
- `droidherd`
   - `namespace` — name of a namespace where you want to run emulators.
   - `sessionValidationExpiredAfterSeconds` — max time that session could exist.
   - `lastSeenMaxDeltaSeconds` — max time that session could exist without a ping.
   - `invalidateSessionsPeriodSeconds` — period of session invalidation.
   - `superuser` — id of the client with superuser rights.
   - `crdTemplatePath` — path where stored template of the CRD using for creating the emulator. See [example](app/src/main/resources/operator/crd-template.yml).
   - `podTemplatePath` — path where stored template of the pod using for creating the emulator. See [example](app/src/main/resources/operator/pod-template.yml).
   - `serviceTemplatePath` — path where stored template of service using for creating the emulator. See [example](app/src/main/resources/operator/service-template.yml).
   - `allowedImages` — map of possible images.
   - `emulatorProxy` — proxy using inside the emulator.
   - `servicePort` — port of the service using for creating the emulator.
   - `dryRun` — enables dry run stage if true.
   - `requeueAfterDefaultSeconds` - operator default reconcile after timeout
   - `requeueAfterPendingSeconds` - operator reconcile after timeout in case of pending emulators (if emulators not ready)
   - `requeueAfterCreationSeconds` - operator reconcile after emulators creation (recommends to set value <= min startup container with emulator)
   - `applyCrdAtStartup` — applies [CRD DroidherdSession](crd-model/src/main/resources/crd/testops.tinkoff.ru_droidherdsessions.yaml) at startup if true.
- `quota`
  - `defaultQuota` — max number of emulators allowed for one client by default.
  - `quotaConfigPath` — path to the config where you can specify quota for each client separately. See [example](app/src/main/resources/quotas/droidherd-client-quotas.conf).
  - `allowMissingConfig` — throw exception on config missing if false.
  - `refreshPeriodMinutes` — period for refreshing the quota from config.

Security config is stored [here](basic-auth/src/main/resources/application.conf).
- `basicAuth`
   - `credentialsPath` — path to the file with credentials. See [example](.deploy/basic-auth/credentials.json).

## Security

In order to demonstrate how Droidherd works we have implemented basic auth scheme. For simplicity, all clients'
credentials are stored in [credentials.json](.deploy/basic-auth/credentials.json).

## How to run

First thing you need to run the service — kubernetes cluster. Without it Droidherd service will fail at startup during 
the dry run stage. 
You can disable dry run (see [Configuration](#configuration)), **but we highly recommend not to do this**.

**Do not forget to specify your namespace and android images** before running (see [Configuration](#configuration)).

## Contributing

See our [CONTRIBUTING.md](CONTRIBUTING.md) guide

## License

🆓 Feel free to use our service in your commercial and private applications.

All DroidHerd components are covered by [Apache 2.0](LICENSE)

Read more about this license [here](https://choosealicense.com/licenses/apache-2.0/)
