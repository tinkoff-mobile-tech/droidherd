package ru.tinkoff.testops.droidherd.service.quota

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import ru.tinkoff.testops.droidherd.api.EmulatorRequest
import ru.tinkoff.testops.droidherd.service.TestDataBuilders.Companion.createResource
import ru.tinkoff.testops.droidherd.service.configs.QuotaConfig
import ru.tinkoff.testops.droidherd.service.models.Session
import java.time.LocalDateTime

class QuotaServiceImplTest {

    private val config = QuotaConfig(10, "test", true, -1)
    private val service = QuotaServiceImpl(config).apply { init() }

    @Test
    fun `quota valid`() {
        val existingSession = Session("client-1", "session-2")

        val session = Session("client-1", "session-1")
        val requests = createRequests(mapOf("image-1" to 6, "image-2" to 1))
        val clientSessions = mapOf(
            existingSession to createResource(existingSession, createRequests(mapOf("image-1" to 3))),
            session to createResource(session, requests)
        )

        val result = service.validate(session, requests, clientSessions)

        result.isValid shouldBe true
        result.requests shouldBe requests
    }

    @Test
    fun `quota exceeded`() {
        val existingSession = Session("client-1", "session-2")
        val clientSessions = mapOf(
            existingSession to createResource(
                existingSession, createRequests(mapOf("image-1" to 6, "image-2" to 1)))
        )

        val session = Session("client-1", "session-1")
        val requests = listOf(
            EmulatorRequest("image-1", 2),
            EmulatorRequest("image-2", 2))
        val result = service.validate(session, requests, clientSessions)

        result.isValid shouldBe false
        result.requests shouldContainAll listOf(
            EmulatorRequest("image-1", 2),
            EmulatorRequest("image-2", 1)
        )
    }

    @Test
    fun `quota exceeded and should be cut from latest sessions - 1`() {
        val session1 = Session("client-1", "session-1")
        val session2 = Session("client-1", "session-2")
        val session3 = Session("client-1", "session-3")
        val session4 = Session("client-1", "session-4")

        val creationTime = LocalDateTime.of(2022, 7, 14, 15, 16, 17)
        val clientSessions = mapOf(
            session1 to createResource(session1, createRequests(mapOf("image-1" to 7)), creationTime),
            session2 to createResource(session2, createRequests(mapOf("image-2" to 2)), creationTime.plusSeconds(2)),
            session3 to createResource(session3, createRequests(mapOf("image-3" to 3)), creationTime.plusSeconds(3)),
            session4 to createResource(session4, createRequests(mapOf("image-4" to 5)), creationTime.plusSeconds(4)),
        )

        val patchedSessions = service.validateBySessionsAge(clientSessions)
            .associate { it.getSession() to it.getRequests() }
        patchedSessions.size shouldBe 2
        patchedSessions[session3]?.shouldContainAll(listOf(EmulatorRequest("image-3", 1)))
        patchedSessions[session4]?.shouldContainAll(listOf(EmulatorRequest("image-4", 0)))
    }

    @Test
    fun `quota exceeded and should be cut from latest sessions - 2`() {
        val session1 = Session("client-1", "session-1")
        val session2 = Session("client-1", "session-2")
        val session3 = Session("client-1", "session-3")

        val creationTime = LocalDateTime.of(2022, 7, 14, 15, 16, 17)
        val clientSessions = mapOf(
            session1 to createResource(session1, createRequests(mapOf("image-1" to 5)), creationTime),
            session2 to createResource(session2, createRequests(mapOf("image-2" to 7)), creationTime.plusSeconds(2)),
            session3 to createResource(session3, createRequests(mapOf("image-3" to 4)), creationTime.plusSeconds(3)),
        )

        val patchedSessions = service.validateBySessionsAge(clientSessions)
            .associate { it.getSession() to it.getRequests() }
        patchedSessions.size shouldBe 2
        patchedSessions[session2]?.shouldContainAll(listOf(EmulatorRequest("image-2", 5)))
        patchedSessions[session3]?.shouldContainAll(listOf(EmulatorRequest("image-3", 0)))
    }

    private fun createRequests(requests: Map<String, Int>): List<EmulatorRequest> {
        return requests.map { EmulatorRequest(it.key, it.value) }
    }
}
