package ru.tinkoff.testops.droidherd.service.kubernetes

import ru.tinkoff.testops.droidherd.models.V1DroidherdSessionStatusEmulators
import ru.tinkoff.testops.droidherd.service.models.DroidherdResource
import ru.tinkoff.testops.droidherd.service.models.EmulatorsRequestData
import ru.tinkoff.testops.droidherd.service.models.Session
import java.time.LocalDateTime

interface KubeService {
    fun getState(): ReadOnlyKubeState

    fun getCrd(name: String): DroidherdResource

    fun createSession(request: EmulatorsRequestData): DroidherdResource

    fun updateCrd(resource: DroidherdResource): Boolean
    fun deleteCrd(session: Session): Boolean

    fun updateState(resource: DroidherdResource)
    fun deleteSessionFromState(session: Session)

    fun updateSessionLastSeen(resource: DroidherdResource, time: LocalDateTime)
    fun updateSessionEmulators(resource: DroidherdResource, emulators: List<V1DroidherdSessionStatusEmulators>)

    fun getEmulators(session: Session): List<V1DroidherdSessionStatusEmulators>
    fun createEmulator(templateParameters: TemplateParameters)
    fun deleteEmulatorsWithIds(ids: List<String>)
}
