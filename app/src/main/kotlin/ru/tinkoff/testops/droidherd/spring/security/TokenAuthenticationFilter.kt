package ru.tinkoff.testops.droidherd.spring.security

import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import org.springframework.security.web.util.matcher.RequestMatcher
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class TokenAuthenticationFilter(matcher: RequestMatcher) : AbstractAuthenticationProcessingFilter(matcher) {
    override fun attemptAuthentication(request: HttpServletRequest, response: HttpServletResponse?): Authentication {
        val token = request.getHeader(AUTHORIZATION) ?: throw BadCredentialsException("Authentication token missed")
        val asAnotherUser = request.getHeader("X-Droidherd-As-Another-Client")
        val auth = UsernamePasswordAuthenticationToken(PrincipalAuthData(asAnotherUser), token)
        return authenticationManager.authenticate(auth)
    }

    override fun successfulAuthentication(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        chain: FilterChain,
        authResult: Authentication?
    ) {
        super.successfulAuthentication(request, response, chain, authResult)
        chain.doFilter(request, response)
    }
}

