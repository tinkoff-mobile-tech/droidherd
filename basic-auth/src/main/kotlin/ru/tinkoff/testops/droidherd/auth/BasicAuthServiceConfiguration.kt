package ru.tinkoff.testops.droidherd.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import java.io.File

@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnMissingBean(value = [AuthService::class])
open class BasicAuthServiceConfiguration {
    @Bean
    open fun basicAuthConfig(): BasicAuthConfig {
        return ConfigFactory.load().extract("basicAuth")
    }

    @Bean
    open fun basicAuthClients(basicAuthConfig: BasicAuthConfig): Map<String, BasicAuthClient> {
        return ObjectMapper()
            .registerKotlinModule()
            .readValue<List<BasicAuthClient>>(File(basicAuthConfig.credentialsPath))
            .associateBy { it.client }
    }

    @Bean
    open fun basicAuthService(basicAuthClients: Map<String, BasicAuthClient>): AuthService {
        return BasicAuthService(basicAuthClients)
    }
}
