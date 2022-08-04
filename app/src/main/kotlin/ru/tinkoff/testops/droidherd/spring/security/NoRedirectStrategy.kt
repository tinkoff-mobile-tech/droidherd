package ru.tinkoff.testops.droidherd.spring.security

import org.springframework.security.web.RedirectStrategy
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class NoRedirectStrategy : RedirectStrategy {
    override fun sendRedirect(request: HttpServletRequest?, response: HttpServletResponse?, url: String?) {
    }
}
