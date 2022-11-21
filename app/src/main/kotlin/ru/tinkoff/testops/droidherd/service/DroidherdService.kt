package ru.tinkoff.testops.droidherd.service

import ru.tinkoff.testops.droidherd.api.*
import ru.tinkoff.testops.droidherd.service.models.DroidherdSystemStatus
import ru.tinkoff.testops.droidherd.service.models.EmulatorsRequestData
import ru.tinkoff.testops.droidherd.service.models.Session
import ru.tinkoff.testops.droidherd.spring.controller.RestApiInternalController.EmulatorStartupMetric

interface DroidherdService {
    fun getSessionStatus(session: Session): DroidherdSessionStatus
    fun getClientStatus(clientId: String): DroidherdClientStatus
    fun getGlobalStatus(): DroidherdStatus

    fun requestEmulators(request: EmulatorsRequestData): List<EmulatorRequest>

    fun release(session: Session)
    fun releaseAll(clientId: String)

    fun ping(session: Session)
    fun postMetrics(session: Session, metrics: List<DroidherdClientMetric>)
    fun dumpState(): DroidherdSystemStatus
    fun getAllowedImages(): Collection<String>

    fun invalidateResources()

    fun postEmulatorStartupMetrics(metric: EmulatorStartupMetric)
}
