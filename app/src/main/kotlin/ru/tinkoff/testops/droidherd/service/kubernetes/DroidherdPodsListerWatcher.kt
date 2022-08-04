package ru.tinkoff.testops.droidherd.service.kubernetes

import com.google.gson.reflect.TypeToken
import io.kubernetes.client.informer.ListerWatcher
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.V1PodList
import io.kubernetes.client.util.CallGeneratorParams
import io.kubernetes.client.util.Watch
import io.kubernetes.client.util.Watchable
import ru.tinkoff.testops.droidherd.service.configs.DroidherdConfig

class DroidherdPodsListerWatcher(
    private val api: CoreV1Api,
    private val config: DroidherdConfig
): ListerWatcher<V1Pod, V1PodList> {
    override fun list(params: CallGeneratorParams?): V1PodList {
        return api.listNamespacedPod(
            config.namespace,
            config.pretty,
            null,
            null,
            null,
            DroidherdReservedLabels.ID,
            null,
            null,
            null,
            config.listResourcesTimeoutSeconds,
            null)
    }

    override fun watch(params: CallGeneratorParams?): Watchable<V1Pod> {
        val call = api.listNamespacedPodCall(
            config.namespace,
            config.pretty,
            null,
            null,
            null,
            DroidherdReservedLabels.ID,
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
            TypeToken.getParameterized(Watch.Response::class.java, V1Pod::class.java).type
        )
    }
}
