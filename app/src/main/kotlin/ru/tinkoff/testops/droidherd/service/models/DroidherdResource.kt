package ru.tinkoff.testops.droidherd.service.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import ru.tinkoff.testops.droidherd.api.CiAttributes
import ru.tinkoff.testops.droidherd.api.Emulator
import ru.tinkoff.testops.droidherd.api.EmulatorRequest
import ru.tinkoff.testops.droidherd.models.V1DroidherdSession
import java.time.LocalDateTime
import java.time.ZoneId

class DroidherdResource(droidherdSessionOriginal: V1DroidherdSession) {
    companion object {
        private val NULL_SESSION = V1DroidherdSession().apply {
            kind("NULL_REPLACEMENT")
        }
        val NULL_RESOURCE = DroidherdResource(NULL_SESSION)
        private val objectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
    }

    private val droidherdSession: V1DroidherdSession =
        if (droidherdSessionOriginal == NULL_SESSION) NULL_SESSION
        else objectMapper.readValue(objectMapper.writeValueAsString(droidherdSessionOriginal))

    private val session = Session(getClientId(), getSessionId())
    private val requests: List<EmulatorRequest> = droidherdSession.spec?.emulators
        ?.map { EmulatorRequest(it.image, it.quantity) }?.toList() ?: listOf()
    private val totalRequestedQuantity = requests.sumOf { it.quantity }

    fun isExist(): Boolean = this != NULL_RESOURCE

    fun getSession() = session
    fun getRequests() = requests
    fun getCrd() = droidherdSession
    fun getEmulatorsFromStatus() = droidherdSession.status?.emulators ?: listOf()
    fun getName(): String = droidherdSession.metadata?.name ?: ""
    fun getCreatedAt(): LocalDateTime {
        return droidherdSession.metadata?.creationTimestamp?.atZoneSameInstant(ZoneId.systemDefault())
            ?.toLocalDateTime() ?: LocalDateTime.MIN
    }
    fun isDebugSession(): Boolean = droidherdSession.spec?.isDebug ?: false
    fun getUid() = droidherdSession.metadata?.uid ?: ""

    fun getCi(): CiAttributes = droidherdSession.spec?.client?.ci?.let {
        CiAttributes(it.name, it.reference, it.repository, it.jobUrl, it.triggeredBy)
    } ?: CiAttributes.EMPTY

    fun getLastSeen(): LocalDateTime {
        return if (!isDebugSession() && droidherdSession.status?.lastSeen == null)
            getCreatedAt()
        else
            parseTime(droidherdSession.status?.lastSeen)
    }

    fun getReadyEmulators(): List<Emulator> = droidherdSession.status?.emulators
        ?.filter { it.ready ?: false }
        ?.map {
            Emulator(it.id, it.image, it.adb, it.extraUris?.associate { e -> e.name to e.uri } ?: mapOf())
        }
        ?.toList() ?: listOf()

    fun getTotalRequestedQuantity() = totalRequestedQuantity

    private fun parseTime(time: String?): LocalDateTime {
        return if (time == null) LocalDateTime.MIN else LocalDateTime.parse(time)
    }

    private fun getSessionId(): String = droidherdSession.spec?.sessionId ?: ""
    private fun getClientId(): String = droidherdSession.spec?.client?.id ?: ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DroidherdResource

        if (droidherdSession != other.droidherdSession) return false

        return true
    }

    override fun hashCode(): Int {
        return droidherdSession.hashCode()
    }
}
