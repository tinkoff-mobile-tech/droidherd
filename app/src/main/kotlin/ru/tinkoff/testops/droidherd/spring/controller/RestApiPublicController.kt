package ru.tinkoff.testops.droidherd.spring.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.tinkoff.testops.droidherd.service.DroidherdService

@RestController
@RequestMapping("api/public")
class RestApiPublicController(private val droidherdService: DroidherdService) {

    @GetMapping("/status")
    fun getGlobalStatus() = droidherdService.getGlobalStatus()

    @GetMapping("/allowed-images")
    fun getAllowedImages() = droidherdService.getAllowedImages()
}
