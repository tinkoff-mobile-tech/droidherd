package ru.tinkoff.testops.droidherd.service.models

data class DroidherdSystemStatus(
    val stateBySession: Map<Session, DroidherdResource>
)
