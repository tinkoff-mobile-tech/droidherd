package ru.tinkoff.testops.droidherd.spring.controller

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class RestExceptionHandler : ResponseEntityExceptionHandler() {
    data class ApiError(
        val message: String
    )

    @ExceptionHandler(value = [IllegalArgumentException::class])
    fun handleIllegalStateOrArgument(
        ex: java.lang.RuntimeException?, request: WebRequest?
    ): ResponseEntity<Any?>? {
        return handleExceptionInternal(
            ex!!,
            ApiError(ex.message ?: "invalid request format"),
            HttpHeaders.EMPTY,
            HttpStatus.BAD_REQUEST,
            request!!
        )
    }
}

