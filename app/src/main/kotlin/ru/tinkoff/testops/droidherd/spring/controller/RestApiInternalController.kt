package ru.tinkoff.testops.droidherd.spring.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.tinkoff.testops.droidherd.service.DroidherdService

@RestController
@RequestMapping("api/internal")
class RestApiInternalController(private val droidherdService: DroidherdService) {
    @GetMapping("/dump-state")
    fun dumpState() = droidherdService.dumpState()
}
