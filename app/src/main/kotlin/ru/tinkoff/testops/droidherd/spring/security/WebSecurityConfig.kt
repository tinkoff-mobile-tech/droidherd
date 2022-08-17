package ru.tinkoff.testops.droidherd.spring.security

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.NegatedRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
open class WebSecurityConfig(private val provider: TokenAuthenticationProvider) : WebSecurityConfigurerAdapter() {
    companion object {
        private val PUBLIC_URLS: RequestMatcher = OrRequestMatcher(
            AntPathRequestMatcher("/api/public/**"),
            AntPathRequestMatcher("/system/**"),
            AntPathRequestMatcher("/v3/api-docs/**"),
            AntPathRequestMatcher("/swagger-ui/**"),
            AntPathRequestMatcher("/swagger-ui.html")
        )
        private val PROTECTED_URLS: RequestMatcher = NegatedRequestMatcher(PUBLIC_URLS)
        private val SUPERUSER_URLS: RequestMatcher = OrRequestMatcher(AntPathRequestMatcher("/api/internal/**"))
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.authenticationProvider(provider)
    }

    override fun configure(http: HttpSecurity) {
        http
            .sessionManagement()
            .sessionCreationPolicy(STATELESS)
            .and()
            .exceptionHandling()
            .defaultAuthenticationEntryPointFor(HttpStatusEntryPoint(FORBIDDEN), PROTECTED_URLS)
            .and()
            .authenticationProvider(provider)
            .addFilterBefore(restAuthenticationFilter(), AnonymousAuthenticationFilter::class.java)
            .authorizeRequests()
            .requestMatchers(SUPERUSER_URLS).hasAuthority("SUPERUSER")
            .requestMatchers(PROTECTED_URLS).authenticated()
            .requestMatchers(PUBLIC_URLS).permitAll()
            .and()
            .csrf().disable()
            .formLogin().disable()
            .httpBasic().disable()
            .logout().disable()
    }

    @Bean
    open fun restAuthenticationFilter(): TokenAuthenticationFilter? {
        val filter = TokenAuthenticationFilter(PROTECTED_URLS)
        filter.setAuthenticationManager(authenticationManager())
        filter.setAuthenticationSuccessHandler(successHandler())
        return filter
    }

    @Bean
    open fun successHandler(): SimpleUrlAuthenticationSuccessHandler? {
        val successHandler = SimpleUrlAuthenticationSuccessHandler()
        successHandler.setRedirectStrategy(NoRedirectStrategy())
        return successHandler
    }

    @Bean
    open fun disableAutoRegistration(filter: TokenAuthenticationFilter?): FilterRegistrationBean<*>? {
        val registration: FilterRegistrationBean<*> = FilterRegistrationBean(filter)
        registration.isEnabled = false
        return registration
    }
}
