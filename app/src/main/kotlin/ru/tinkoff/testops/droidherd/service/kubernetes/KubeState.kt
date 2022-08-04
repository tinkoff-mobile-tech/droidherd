package ru.tinkoff.testops.droidherd.service.kubernetes

import ru.tinkoff.testops.droidherd.service.models.DroidherdResource
import ru.tinkoff.testops.droidherd.service.models.Session

class KubeState(
    private val stateByClient: Map<String, Map<Session, DroidherdResource>>
): ReadOnlyKubeState {
    override fun get(clientId: String): Map<Session, DroidherdResource> = stateByClient.getOrDefault(clientId, mapOf())

    override fun get(session: Session): DroidherdResource = get(session.clientId)[session] ?: DroidherdResource.NULL_RESOURCE

    override fun getAllResources(): Set<DroidherdResource> = stateByClient.values
        .map {
            it.values
        }.flatten().toSet()

    override fun getAllResourcesByClient() = stateByClient

    override fun containsClient(clientId: String) = stateByClient.containsKey(clientId)

    override fun containsSession(session: Session) =
        stateByClient.getOrDefault(session.clientId, mapOf()).containsKey(session)

    fun add(resource: DroidherdResource): KubeState {
        val value = stateByClient.getOrDefault(resource.getSession().clientId, mapOf()).plus(resource.getSession() to resource)

        return KubeState(
            stateByClient + (resource.getSession().clientId to value))
    }

    fun delete(session: Session) = KubeState(
        stateByClient + (session.clientId to (get(session.clientId) - session)))
}
