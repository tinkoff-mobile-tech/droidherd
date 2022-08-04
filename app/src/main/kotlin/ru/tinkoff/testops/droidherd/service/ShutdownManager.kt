package ru.tinkoff.testops.droidherd.service

interface ShutdownManager {
    fun shutdown(exitCode: Int)
}
