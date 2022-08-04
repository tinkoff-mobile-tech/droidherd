package ru.tinkoff.testops.droidherd.spring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["ru.tinkoff.testops.droidherd"])
open class AppRunner

fun main(args: Array<String>) {
    runApplication<AppRunner>(*args)
}
