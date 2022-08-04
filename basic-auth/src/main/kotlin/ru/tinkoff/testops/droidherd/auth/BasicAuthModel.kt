package ru.tinkoff.testops.droidherd.auth

data class BasicAuthClient(
    val client: String,
    val password: String,
    val info: String = "",
)

data class BasicAuthConfig(
    val credentialsPath: String
)
