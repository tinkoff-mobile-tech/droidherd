package ru.tinkoff.testops.droidherd.service.quota

import ru.tinkoff.testops.droidherd.api.EmulatorRequest

data class QuotaResult(
    val isValid: Boolean,
    val requests: List<EmulatorRequest>
)
