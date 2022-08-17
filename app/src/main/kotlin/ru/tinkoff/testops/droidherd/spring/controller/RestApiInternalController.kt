package ru.tinkoff.testops.droidherd.spring.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.tinkoff.testops.droidherd.service.DroidherdService

@RestController
@RequestMapping("api/internal")
@Tags(Tag(name = "RestApiInternalController", description = "for SUPERUSERs only"))
class RestApiInternalController(private val droidherdService: DroidherdService) {
    @Operation(summary = "Dump state (for SUPERUSER only)")
    @GetMapping("/dump-state")
    fun dumpState() = droidherdService.dumpState()
}
