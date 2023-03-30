package ru.tinkoff.testops.droidherd.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnMissingBean(value = [AuthService::class])
open class BasicAuthServiceConfiguration {
    companion object {
        val CREDS_PATH = "BASIC_AUTH_CREDENTIALS_PATH"
        val NOT_EXIST_PATH = "#not-exist#"
    }

    @Bean
    open fun basicAuthClients(): Map<String, BasicAuthClient> {
        val credentialsPath = System.getenv(CREDS_PATH) ?: NOT_EXIST_PATH
        if (credentialsPath == NOT_EXIST_PATH || !Files.isRegularFile(Path.of(credentialsPath))) {
            throw RuntimeException("No $CREDS_PATH env var specified or file not exist: $credentialsPath")
        }
        return ObjectMapper()
            .registerKotlinModule()
            .readValue<List<BasicAuthClient>>(File(credentialsPath))
            .associateBy { it.client }
    }

    @Bean
    open fun basicAuthService(basicAuthClients: Map<String, BasicAuthClient>): AuthService {
        return BasicAuthService(basicAuthClients)
    }
}
