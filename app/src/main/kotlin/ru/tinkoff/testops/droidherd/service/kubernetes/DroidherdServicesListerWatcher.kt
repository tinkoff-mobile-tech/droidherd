package ru.tinkoff.testops.droidherd.service.kubernetes

import com.google.gson.reflect.TypeToken
import io.kubernetes.client.informer.ListerWatcher
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1Service
import io.kubernetes.client.openapi.models.V1ServiceList
import io.kubernetes.client.util.CallGeneratorParams
import io.kubernetes.client.util.Watch
import io.kubernetes.client.util.Watchable
import ru.tinkoff.testops.droidherd.service.configs.DroidherdConfig

class DroidherdServicesListerWatcher(
    private val api: CoreV1Api,
    private val config: DroidherdConfig
): ListerWatcher<V1Service, V1ServiceList> {
    override fun list(params: CallGeneratorParams?): V1ServiceList {
        return api.listNamespacedService(
            config.namespace,
            config.pretty,
            null,
            null,
            null,
            "droidherdId",
            null,
            null,
            null,
            config.listResourcesTimeoutSeconds,
            null)
    }

    override fun watch(params: CallGeneratorParams?): Watchable<V1Service> {
        val call = api.listNamespacedServiceCall(
            config.namespace,
            config.pretty,
            null,
            null,
            null,
            "droidherdId",
            null,
            null,
            null,
            config.listResourcesTimeoutSeconds,
            true,
            null
        )

        return Watch.createWatch(
            api.apiClient,
            call,
            TypeToken.getParameterized(Watch.Response::class.java, V1Service::class.java).type
        )
    }
}
