package ru.tinkoff.testops.droidherd.service.kubernetes

interface DroidherdReservedLabels {
    companion object {
        const val ID = "droidherdId"
        const val SESSION_ID = "droidherdSessionId"
        const val CLIENT_ID = "droidherdClientId"
    }
}
