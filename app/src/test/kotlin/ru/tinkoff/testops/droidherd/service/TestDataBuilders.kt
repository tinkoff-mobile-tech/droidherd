package ru.tinkoff.testops.droidherd.service

import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.mockk.every
import io.mockk.spyk
import ru.tinkoff.testops.droidherd.api.ClientAttributes
import ru.tinkoff.testops.droidherd.api.EmulatorRequest
import ru.tinkoff.testops.droidherd.api.SessionRequest
import ru.tinkoff.testops.droidherd.models.*
import ru.tinkoff.testops.droidherd.service.models.DroidherdResource
import ru.tinkoff.testops.droidherd.service.models.EmulatorsRequestData
import ru.tinkoff.testops.droidherd.service.models.Session
import java.time.LocalDateTime

class TestDataBuilders {

    companion object {
        fun createResource(
            session: Session,
            requests: List<EmulatorRequest>,
            creationTime: LocalDateTime = LocalDateTime.MIN
        ): DroidherdResource {
            val crd = V1DroidherdSession().apply {
                metadata = V1ObjectMeta()
                spec = V1DroidherdSessionSpec().apply {
                    client = V1DroidherdSessionSpecClient().apply {
                        id = session.clientId
                    }
                    sessionId = session.sessionId
                    emulators = requests.map {
                        V1DroidherdSessionSpecEmulators().apply {
                            image = it.image
                            quantity = it.quantity
                        }
                    }
                }
                status = V1DroidherdSessionStatus()
            }
            val mock = spyk(DroidherdResource(crd))
            every { mock.getSession() } answers { callOriginal() }
            every { mock.getTotalRequestedQuantity() } answers { callOriginal() }
            every { mock.getCreatedAt() } returns creationTime
            return mock
        }

        fun createRequest(session: Session, requests: List<EmulatorRequest>): EmulatorsRequestData {
            return EmulatorsRequestData(
                session,
                "1",
                SessionRequest(
                    ClientAttributes("1", "test", null, mapOf()),
                    requests,
                    listOf(),
                    false
                )
            )
        }
    }
}
