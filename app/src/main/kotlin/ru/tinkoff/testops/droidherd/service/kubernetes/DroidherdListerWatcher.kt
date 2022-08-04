package ru.tinkoff.testops.droidherd.service.kubernetes

import io.kubernetes.client.informer.ListerWatcher
import io.kubernetes.client.util.CallGeneratorParams
import io.kubernetes.client.util.Watchable
import io.kubernetes.client.util.generic.options.ListOptions
import ru.tinkoff.testops.droidherd.models.V1DroidherdSession
import ru.tinkoff.testops.droidherd.models.V1DroidherdSessionList
import ru.tinkoff.testops.droidherd.service.configs.DroidherdConfig

class DroidherdListerWatcher(
    private val api: V1DroidherdSessionApi,
    private val config: DroidherdConfig
): ListerWatcher<V1DroidherdSession, V1DroidherdSessionList> {
    override fun list(params: CallGeneratorParams?): V1DroidherdSessionList {
        return api
            .list(
                config.namespace,
                ListOptions().apply {
                    params?.let {
                        resourceVersion = it.resourceVersion
                        timeoutSeconds = it.timeoutSeconds
                    }

                }
            )
            .throwsApiException()
            .getObject()
    }

    override fun watch(params: CallGeneratorParams?): Watchable<V1DroidherdSession> {
        return api.watch(config.namespace, ListOptions().apply {
            params?.let {
                resourceVersion = it.resourceVersion
                timeoutSeconds = it.timeoutSeconds
            }
        })
    }
}
