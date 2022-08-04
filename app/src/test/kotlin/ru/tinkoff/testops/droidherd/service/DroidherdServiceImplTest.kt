package ru.tinkoff.testops.droidherd.service

import io.kotest.matchers.collections.shouldContainAll
import io.mockk.every
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import org.junit.jupiter.api.Test
import ru.tinkoff.testops.droidherd.api.EmulatorRequest
import ru.tinkoff.testops.droidherd.service.configs.DroidherdConfig
import ru.tinkoff.testops.droidherd.service.kubernetes.KubeService
import ru.tinkoff.testops.droidherd.service.kubernetes.KubeState
import ru.tinkoff.testops.droidherd.service.models.Session
import ru.tinkoff.testops.droidherd.service.quota.QuotaResult
import ru.tinkoff.testops.droidherd.service.quota.QuotaService

class DroidherdServiceImplTest {

    private val config = mockk<DroidherdConfig>().apply {
        every { allowedImages } returns mapOf("image-1" to "image-1")
    }

    @Test
    fun `duplicate request for same session return original state`() {
        val kubeService = mockk<KubeService>()
        val quotaService = mockk<QuotaService>()
        val service = DroidherdServiceImpl(config, kubeService, quotaService, CollectorRegistry())
        val requestsInState = listOf(EmulatorRequest("image-1", 2))
        val session = Session("client-1", "session_1")
        val kubeState = KubeState(mapOf("client-1" to mapOf(session to TestDataBuilders.createResource(
            session, requestsInState
        ))))
        every { kubeService.getState() } returns kubeState

        val request = TestDataBuilders.createRequest(
            session, listOf(EmulatorRequest("image-1", 3)))
        service.requestEmulators(request) shouldContainAll requestsInState
    }

    @Test
    fun `request with excess quota automatically patch quantity`() {
        val kubeService = mockk<KubeService>()
        val quotaService = mockk<QuotaService>()
        val service = DroidherdServiceImpl(config, kubeService, quotaService, CollectorRegistry())
        val quotaPatchedRequests = listOf(EmulatorRequest("image-1", 2))
        val session = Session("client-1", "session_1")
        val kubeState = KubeState(mapOf())
        every { kubeService.getState() } returns kubeState
        every { kubeService.createSession(any()) } returns mockk()
        every { quotaService.validate(any(), any(), any()) } returns
            QuotaResult(false, quotaPatchedRequests)

        val request = TestDataBuilders.createRequest(
            session, listOf(EmulatorRequest("image-1", 3)))
        service.requestEmulators(request) shouldContainAll quotaPatchedRequests
    }

    @Test
    fun `invalidate resources cleanup expired sessions`() {
        // todo
    }

    @Test
    fun `invalidate resources cleanup out of keep-alive sessions`() {
        // todo
    }

}
