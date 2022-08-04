package ru.tinkoff.testops.droidherd.auth

class BasicAuthData(
    private val isAuthorized: Boolean,
    private val clientId: String,
) : AuthData {
    override fun isAuthorized() = isAuthorized
    override fun getClientId() = clientId
}
