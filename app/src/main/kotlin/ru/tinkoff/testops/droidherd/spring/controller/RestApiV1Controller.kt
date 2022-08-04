package ru.tinkoff.testops.droidherd.spring.controller

import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import ru.tinkoff.testops.droidherd.api.*
import ru.tinkoff.testops.droidherd.service.DroidherdService
import ru.tinkoff.testops.droidherd.service.models.EmulatorsRequestData
import ru.tinkoff.testops.droidherd.service.models.Session
import ru.tinkoff.testops.droidherd.spring.security.DroidherdAuthDetails
import java.util.*
import javax.servlet.ServletRequest


@RestController
@RequestMapping("api/v1")
class RestApiV1Controller(private val droidherdService: DroidherdService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/clients/{clientId}/login")
    fun login(
        @AuthenticationPrincipal authDetails: DroidherdAuthDetails,
        @PathVariable clientId: String
    ): Session {
        validateClient(authDetails, clientId)
        val uuid = UUID.randomUUID()
        val sessionId = "%x%x".format(
            uuid.mostSignificantBits.hashCode(), uuid.leastSignificantBits.hashCode()
        )
        val session = Session(authDetails.clientId, sessionId)
        log.info("$session logged in")
        return session
    }

    @GetMapping("/clients/{clientId}/sessions/{sessionId}")
    fun getSessionStatus(
        @AuthenticationPrincipal authDetails: DroidherdAuthDetails,
        @PathVariable clientId: String,
        @PathVariable sessionId: String
    ): DroidherdSessionStatus {
        validateClient(authDetails, clientId)
        val session = Session(authDetails.clientId, sessionId)
        log.info("$session asked for session status")
        return droidherdService.getSessionStatus(session)
    }

    @GetMapping("/clients/{clientId}/sessions")
    fun getClientStatus(
        @AuthenticationPrincipal authDetails: DroidherdAuthDetails,
        @PathVariable clientId: String,
    ): DroidherdClientStatus {
        validateClient(authDetails, clientId)
        log.info("Client $clientId asked for status")
        return droidherdService.getClientStatus(authDetails.clientId)
    }

    @PostMapping("/clients/{clientId}/sessions/{sessionId}")
    fun requestEmulators(
        @AuthenticationPrincipal authDetails: DroidherdAuthDetails,
        @PathVariable clientId: String,
        @PathVariable sessionId: String,
        @RequestBody requests: SessionRequest,
        servletRequest: ServletRequest
    ): List<EmulatorRequest> {
        validateClient(authDetails, clientId)
        val session = Session(authDetails.clientId, sessionId)
        val clientIp = servletRequest.remoteAddr
        log.info("$session with ip $clientIp has requested emulators : $requests")
        val request = EmulatorsRequestData(
            session,
            clientIp,
            requests
        )
        return droidherdService.requestEmulators(request)
    }

    @DeleteMapping("/clients/{clientId}/sessions/{sessionId}")
    fun releaseSession(
        @AuthenticationPrincipal authDetails: DroidherdAuthDetails,
        @PathVariable clientId: String,
        @PathVariable sessionId: String,
    ) {
        validateClient(authDetails, clientId)
        val session = Session(authDetails.clientId, sessionId)
        log.info("$session has requested release")
        droidherdService.release(session)
    }

    @DeleteMapping("/clients/{clientId}/sessions")
    fun releaseAllSessions(
        @AuthenticationPrincipal authDetails: DroidherdAuthDetails,
        @PathVariable clientId: String
    ) {
        validateClient(authDetails, clientId)
        log.info("Client ${authDetails.clientId} has requested release for all sessions")
        droidherdService.releaseAll(authDetails.clientId)
    }

    @PostMapping("/clients/{clientId}/sessions/{sessionId}/ping")
    fun ping(
        @AuthenticationPrincipal authDetails: DroidherdAuthDetails,
        @PathVariable sessionId: String,
        @PathVariable clientId: String,
    ) {
        validateClient(authDetails, clientId)
        val session = Session(authDetails.clientId, sessionId)
        log.info("Got ping from $session")
        droidherdService.ping(session)
    }

    @PostMapping("/clients/{clientId}/sessions/{sessionId}/metrics")
    fun postMetrics(
        @AuthenticationPrincipal authDetails: DroidherdAuthDetails,
        @PathVariable sessionId: String,
        @PathVariable clientId: String,
        @RequestBody metrics: List<DroidherdClientMetric>,
    ) {
        validateClient(authDetails, clientId)
        val session = Session(authDetails.clientId, sessionId)
        log.info("Got metrics from $session")
        droidherdService.postMetrics(session, metrics)
    }

    private fun validateClient(
        authDetails: DroidherdAuthDetails,
        clientId: String
    ) {
        if (authDetails.getUnderlyingClientId() != clientId) {
            throw IllegalArgumentException("ClientId not match in request($clientId) and authentication data(${authDetails.getUnderlyingClientId()})")
        }
    }
}
