package ru.tinkoff.testops.droidherd.auth

import org.springframework.util.Base64Utils

class BasicAuthService(private val basicAuthClients: Map<String, BasicAuthClient>) : AuthService {
    override fun doAuth(token: String): AuthData {
        val processedToken = token.substringAfter("Basic ")
        if (processedToken.isBlank()) {
            throw IllegalArgumentException("Invalid authorization header, can't authorize: [${processedToken}]")
        }
        val decodedToken = String(Base64Utils.decodeFromString(processedToken))
        val (clientId, password) = decodedToken.split(":")
        return BasicAuthData(isValid(clientId, password), clientId)
    }

    private fun isValid(client: String, password: String): Boolean {
        val entry = basicAuthClients[client]
        return entry?.client == client
            && entry.password == password
    }
}
