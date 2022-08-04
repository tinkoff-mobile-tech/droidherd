package ru.tinkoff.testops.droidherd.auth

interface AuthData {
    fun isAuthorized(): Boolean
    fun getClientId(): String
}
