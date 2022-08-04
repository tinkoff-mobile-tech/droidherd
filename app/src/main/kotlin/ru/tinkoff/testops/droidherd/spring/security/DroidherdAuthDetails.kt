package ru.tinkoff.testops.droidherd.spring.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import ru.tinkoff.testops.droidherd.auth.AuthData

class DroidherdAuthDetails(
    val clientId: String,
    private val authData: AuthData,
    private val token: String,
    private val isSuperuser: Boolean) : UserDetails {
    companion object {
        private val SUPERUSER_AUTHORITY = listOf(SimpleGrantedAuthority("SUPERUSER"))
        private val REGULAR_USER_AUTHORITY = listOf(SimpleGrantedAuthority("USER"))
    }

    fun getUnderlyingClientId() = authData.getClientId()

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return if (isSuperuser) SUPERUSER_AUTHORITY else REGULAR_USER_AUTHORITY
    }

    override fun getPassword(): String {
        return token
    }

    override fun getUsername(): String {
        return clientId
    }

    override fun isAccountNonExpired(): Boolean {
        return authData.isAuthorized()
    }

    override fun isAccountNonLocked(): Boolean {
        return authData.isAuthorized()
    }

    override fun isCredentialsNonExpired(): Boolean {
        return authData.isAuthorized()
    }

    override fun isEnabled(): Boolean {
        return authData.isAuthorized()
    }
}
