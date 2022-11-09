package ru.tinkoff.testops.droidherd.service.quota

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import ru.tinkoff.testops.droidherd.api.EmulatorRequest
import ru.tinkoff.testops.droidherd.models.V1DroidherdSessionSpecEmulators
import ru.tinkoff.testops.droidherd.service.configs.QuotaConfig
import ru.tinkoff.testops.droidherd.service.models.DroidherdResource
import ru.tinkoff.testops.droidherd.service.models.Session
import java.io.File
import java.time.Duration
import kotlin.math.max

class QuotaServiceImpl(
    private val config: QuotaConfig
) : QuotaService {

    private val log = LoggerFactory.getLogger(javaClass)

    fun init() {
        try {
            refresh()
        } catch (e: Exception) {
            throw RuntimeException("Failed to refresh client quotas", e)
        }

        if (config.refreshPeriodMinutes > 0) {
            GlobalScope.launch(Dispatchers.IO) {
                val ticker = ticker(Duration.ofMinutes(config.refreshPeriodMinutes).toMillis())
                for (tick in ticker) {
                    runCatching { refresh() }
                        .onFailure { log.error("Failed to refresh client quotas", it) }
                }
            }
        } else {
            log.info("Client quota configuration refresh skipped")
        }
    }

    @Volatile
    private var quotaByClientMap: Map<String, Int> = mapOf()

    override fun get(clientId: String): Int {
        return quotaByClientMap.getOrDefault(clientId, config.defaultQuota)
    }

    private fun refresh() {
        log.debug("Client quotas refresh triggered")
        val quotaConfig =
            ConfigFactory.parseFile(
                File(config.quotaConfigPath),
                ConfigParseOptions.defaults().setAllowMissing(config.allowMissingConfig))
        if (!quotaConfig.isEmpty) {
            val newQuotaByClientMap =
                quotaConfig.getObject("clients").entries.associate { it.key to it.value.unwrapped() as Int }
            if (quotaByClientMap != newQuotaByClientMap) {
                quotaByClientMap = newQuotaByClientMap
                log.info("Client quotas refreshed, entries: {}", quotaByClientMap.size)
            }
        } else {
            quotaByClientMap = mapOf()
        }
    }

    override fun validate(
        session: Session,
        requests: List<EmulatorRequest>,
        clientSessions: Map<Session, DroidherdResource>
    ): QuotaResult {
        val clientId = session.clientId
        val spentQuota = clientSessions
            .filter { it.key != session }
            .entries.sumOf { it.value.getTotalRequestedQuantity() }
        val totalQuota = get(clientId)
        val requested = requests.sumOf { it.quantity }

        if (spentQuota + requested > totalQuota) {
            log.warn("{} exceeded its quota: {} + {} > {} (spent + requested > total)",
                session, spentQuota, requested, totalQuota)
            val remaining = max(0, totalQuota - spentQuota)
            val patchedRequests = patchRequests(session, requests, remaining)
            return QuotaResult(false, patchedRequests)
        }
        return QuotaResult(true, requests)
    }

    override fun validateBySessionsAge(sessionsById: Map<Session, DroidherdResource>): List<DroidherdResource> {
        val sessionByIdMutable = sessionsById.toMutableMap()
        val sortedSessions = sessionByIdMutable.values
            .sortedWith(compareBy({ it.getCreatedAt() }, { it.getSession().sessionId }))
            .reversed()

        return sortedSessions.mapNotNull {
            val result = validate(it.getSession(), it.getRequests(), sessionByIdMutable)
            sessionByIdMutable.remove(it.getSession())
            val patchedResource = if (result.isValid) it else DroidherdResource(it.getCrd().apply {
                spec!!.emulators = result.requests.map {
                    V1DroidherdSessionSpecEmulators().apply {
                        image = it.image
                        quantity = it.quantity
                    }
                }
            })
            if (!result.isValid) patchedResource else null
        }
    }

    private fun patchRequests(session: Session, requests: List<EmulatorRequest>, remaining: Int): List<EmulatorRequest> {
        var accumulated = 0
        return requests.map {
            val patchedRequestQuantity = patchRequestQuantity(session, it, accumulated, remaining)
            accumulated += patchedRequestQuantity
            EmulatorRequest(it.image, patchedRequestQuantity)
        }
    }

    private fun patchRequestQuantity(session: Session, request: EmulatorRequest, accumulated: Int, remaining: Int): Int {
        if (request.quantity > remaining - accumulated) {
            val newQuantity = max(0, remaining - accumulated)
            log.warn("{} requested {} and exceeded its quota. New quantity {} will be set for {}",
                session, request.quantity, newQuantity, request.image)
            return newQuantity
        }
        return request.quantity
    }
}
