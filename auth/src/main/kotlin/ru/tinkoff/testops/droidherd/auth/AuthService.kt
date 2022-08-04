package ru.tinkoff.testops.droidherd.auth

interface AuthService {
    fun doAuth(token: String): AuthData
}
