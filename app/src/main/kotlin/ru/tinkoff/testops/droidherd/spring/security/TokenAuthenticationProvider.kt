package ru.tinkoff.testops.droidherd.spring.security

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import ru.tinkoff.testops.droidherd.auth.AuthData
import ru.tinkoff.testops.droidherd.auth.AuthService
import ru.tinkoff.testops.droidherd.auth.UnauthorizedException
import ru.tinkoff.testops.droidherd.service.configs.DroidherdConfig

@Component
class TokenAuthenticationProvider(
    private val authService: AuthService,
    droidherdConfig: DroidherdConfig
    ) : AbstractUserDetailsAuthenticationProvider() {

    private val log = LoggerFactory.getLogger(javaClass)

    private val superuser: String = droidherdConfig.superuser

    override fun additionalAuthenticationChecks(userDetails: UserDetails?, authentication: UsernamePasswordAuthenticationToken?) {
    }

    override fun retrieveUser(username: String, authentication: UsernamePasswordAuthenticationToken): UserDetails {
        val principalAuthData = authentication.principal as PrincipalAuthData
        val token = authentication.credentials as String
        try {
            val authData = authService.doAuth(token)
            if (!authData.isAuthorized()) {
                throw BadCredentialsException("Client authorization failed, bad credentials: ${authData.getClientId()}")
            }
            val isSuperuser = isSuperuser(authData)
            val asClientId = principalAuthData.asAnotherClient?.let {
                if (isSuperuser) {
                    it
                } else {
                    throw IllegalArgumentException("As another client functionality allowed only for superuser, client: ${authData.getClientId()}")
                }
            } ?: authData.getClientId()
            return DroidherdAuthDetails(asClientId, authData, token, isSuperuser)
        } catch (e: RuntimeException) {
            if (e !is UnauthorizedException) {
                log.error("Error during authenticating the client", e)
            }
            throw BadCredentialsException(e.message, e)
        }
    }

    private fun isSuperuser(authData: AuthData): Boolean {
        if (superuser.isNotBlank()) {
            return authData.getClientId() == superuser
        }
        return false
    }
}

