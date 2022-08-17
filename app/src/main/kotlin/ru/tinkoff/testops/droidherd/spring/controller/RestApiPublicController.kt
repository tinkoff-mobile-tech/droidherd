package ru.tinkoff.testops.droidherd.spring.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.tinkoff.testops.droidherd.service.DroidherdService

@RestController
@RequestMapping("api/public")
@Tags(Tag(name = "RestApiPublicController", description = "allowed without authorization"))
class RestApiPublicController(private val droidherdService: DroidherdService) {

    @GetMapping("/status")
    @Operation(summary = "Get global status")
    fun getGlobalStatus() = droidherdService.getGlobalStatus()

    @GetMapping("/allowed-images")
    @Operation(summary = "Get allowed images")
    fun getAllowedImages() = droidherdService.getAllowedImages()
}
