package ru.tinkoff.testops.droidherd.service.kubernetes

import ru.tinkoff.testops.droidherd.service.models.DroidherdResource
import ru.tinkoff.testops.droidherd.service.models.Session

interface ReadOnlyKubeState {
    fun get(clientId: String): Map<Session, DroidherdResource>
    fun get(session: Session): DroidherdResource
    fun getAllResourcesByClient(): Map<String, Map<Session, DroidherdResource>>
    fun getAllResources(): Set<DroidherdResource>

    fun containsClient(clientId: String): Boolean
    fun containsSession(session: Session): Boolean
}
