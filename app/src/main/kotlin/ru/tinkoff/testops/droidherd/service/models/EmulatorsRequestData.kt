package ru.tinkoff.testops.droidherd.service.models

import ru.tinkoff.testops.droidherd.api.SessionRequest

data class EmulatorsRequestData(
    val session: Session,
    val clientIp: String,
    val data: SessionRequest
)
