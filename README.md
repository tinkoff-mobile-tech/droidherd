# Droidherd
DroidHerd service - k8s android farm orchestration service.

## What is it
DroidHerd is service which manage android farm in k8s and provide single interface to clients to use it with support following functionality:
- authorization
- multiple sessions
- quotas for clients
- API to be able customize running emulators for each client

Android emulators itself is not part of service - you can use anyone which you found or develop by self.

## How it works
Service contain 2 main components - rest controller to serve API requests from clients (and admin) and operator.

Clients make request to service using API to get required count of specified version of emulators which supported by service.

Service works as k8s operator and each request transforms to CRD and then service works with CRD to reach desired state (run required emulators).

Service cloud-native and support multiple replicas and additionally supports quota for clients and sessions - so clients can create multiple sessions but service
will be monitor quota usage and restict quota exceeding.

## Clients API

TBD

## Configuration

TBD

## Security

TBD

## How to run

### Run in k8s
TBD

### Run locally for testing/debugging
TBD

## Contributing

See our [CONTRIBUTING.md](/CONTRIBUTING.md) guide

## License

🆓 Feel free to use our service in your commercial and private applications.

All DroidHerd components are covered by [Apache 2.0](/LICENSE)

Read more about this license [here](https://choosealicense.com/licenses/apache-2.0/)
