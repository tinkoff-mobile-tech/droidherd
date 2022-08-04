package ru.tinkoff.testops.droidherd.service.quota

import ru.tinkoff.testops.droidherd.api.EmulatorRequest
import ru.tinkoff.testops.droidherd.service.models.DroidherdResource
import ru.tinkoff.testops.droidherd.service.models.Session

interface QuotaService {
    fun get(clientId: String): Int
    fun validate(
        session: Session,
        requests: List<EmulatorRequest>,
        clientSessions: Map<Session, DroidherdResource>
    ): QuotaResult

    fun validateBySessionsAge(sessionsById: Map<Session, DroidherdResource>
    ): List<DroidherdResource>
}
