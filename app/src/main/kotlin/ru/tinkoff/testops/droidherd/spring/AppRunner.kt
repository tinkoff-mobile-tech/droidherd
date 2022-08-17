package ru.tinkoff.testops.droidherd.spring

import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["ru.tinkoff.testops.droidherd"])
@SecurityScheme(type = SecuritySchemeType.HTTP, name = "basicAuth", scheme = "basic")
@OpenAPIDefinition(
    info = Info(
        title = "Droidherd",
        description = "k8s android farm orchestration service",
        license = License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
    ),
    externalDocs = ExternalDocumentation(description = "GitHub", url = "https://github.com/Tinkoff/droidherd")
)
open class AppRunner

fun main(args: Array<String>) {
    runApplication<AppRunner>(*args)
}
