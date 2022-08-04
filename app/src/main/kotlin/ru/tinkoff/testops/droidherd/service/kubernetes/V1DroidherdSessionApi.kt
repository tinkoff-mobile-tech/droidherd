package ru.tinkoff.testops.droidherd.service.kubernetes

import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.util.generic.GenericKubernetesApi
import ru.tinkoff.testops.droidherd.models.V1DroidherdSession
import ru.tinkoff.testops.droidherd.models.V1DroidherdSessionList

class V1DroidherdSessionApi(
    apiClient: ApiClient
) : GenericKubernetesApi<V1DroidherdSession, V1DroidherdSessionList>(
    V1DroidherdSession::class.java, V1DroidherdSessionList::class.java, "testops.tinkoff.ru", "v1", "droidherdsessions", apiClient
)
