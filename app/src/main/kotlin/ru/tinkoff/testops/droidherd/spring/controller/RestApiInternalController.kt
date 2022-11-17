package ru.tinkoff.testops.droidherd.spring.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.web.bind.annotation.*
import ru.tinkoff.testops.droidherd.service.DroidherdService

@RestController
@RequestMapping("api/internal")
@Tags(Tag(name = "RestApiInternalController", description = "with access only from cluster"))
class RestApiInternalController(private val droidherdService: DroidherdService) {
    @Operation(summary = "Dump state (for SUPERUSER only)")
    @GetMapping("/dump-state")
    fun dumpState() = droidherdService.dumpState()

    @PostMapping("/startup-metric")
    @Operation(summary = "Post startup metric (for emulators)")
    fun postEmulatorStartupTime(@RequestBody metric: EmulatorStartupMetric) {
        droidherdService.postEmulatorStartupMetrics(metric)
    }

    data class EmulatorStartupMetric(
        val image: String,
        val value: Double
    )
}
