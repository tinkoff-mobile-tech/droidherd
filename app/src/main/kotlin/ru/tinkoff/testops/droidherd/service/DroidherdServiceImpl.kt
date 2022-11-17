package ru.tinkoff.testops.droidherd.service

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.tinkoff.testops.droidherd.api.*
import ru.tinkoff.testops.droidherd.service.configs.DroidherdConfig
import ru.tinkoff.testops.droidherd.service.kubernetes.KubeService
import ru.tinkoff.testops.droidherd.service.kubernetes.ReadOnlyKubeState
import ru.tinkoff.testops.droidherd.service.models.DroidherdResource
import ru.tinkoff.testops.droidherd.service.models.DroidherdSystemStatus
import ru.tinkoff.testops.droidherd.service.models.EmulatorsRequestData
import ru.tinkoff.testops.droidherd.service.models.Session
import ru.tinkoff.testops.droidherd.service.quota.QuotaService
import ru.tinkoff.testops.droidherd.spring.controller.RestApiInternalController.EmulatorStartupMetric
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DroidherdServiceImpl(
    private val config: DroidherdConfig,
    private val kubeService: KubeService,
    private val quotaService: QuotaService,
    registry: CollectorRegistry = CollectorRegistry.defaultRegistry
) : DroidherdService {
    companion object {
        private const val MAX_SESSION_ID_LENGTH = 32
        private const val MAX_K8S_LABEL_LENGTH = 63
        private const val MAX_CLIENT_INFO_LENGTH = 128
        private const val MAX_CLIENT_VERSION_LENGTH = 128
        private const val MAX_CI_URL_LENGTH = 1024
        private const val MAX_PARAMETER_NAME_LENGTH = 55
        private const val MAX_PARAMETER_LENGTH = 8192
        private val ALPHANUM_REGEX = Regex("^[a-zA-Z0-9-_]+\$")
        private val ALPHANUM_REGEX_WITHOUT_HYPHEN = Regex("^[a-zA-Z0-9_]+\$")
    }

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val emulatorsRequestedCounter = Counter.build()
        .name("emulators_requested_total")
        .help("Number of emulators requested per client/image over time")
        .labelNames("client", "image")
        .register(registry)

    private val sessionsExpiredTotal = Counter.build()
        .name("sessions_expired_total")
        .help("Number of expired per client over time")
        .labelNames("client")
        .register(registry)

    private val emptySessionsTotal = Counter.build()
        .name("empty_sessions_total")
        .help("Number of failures in session creation (connected with CTQA-935)")
        .labelNames("client")
        .register(registry)

    private val emulatorStartupTime = Gauge.build()
        .name("emulator_startup_time")
        .help("Emulator startup time per image")
        .labelNames("image")
        .register(registry)

    fun init() {
        GlobalScope.launch(Dispatchers.IO) {
            val ticker = ticker(Duration.ofSeconds(config.invalidateSessionsPeriodSeconds).toMillis())
            for (tick in ticker) {
                runCatching {
                    invalidateResources()
                }.onFailure { log.error("Failed to invalidate sessions", it) }
            }
        }
    }

    override fun getSessionStatus(session: Session): DroidherdSessionStatus =
        DroidherdSessionStatus(
            if (!kubeService.getState().containsSession(session)) listOf()
            else kubeService.getState().get(session).getReadyEmulators()
        ).also {
            log.info("get session {}, emulators count: {}", session, it.emulators.size)
        }


    override fun getClientStatus(clientId: String): DroidherdClientStatus =
        if (kubeService.getState().containsClient(clientId)) {
            val state = kubeService.getState().get(clientId)
            val totalRequested = state.map { (session, state) ->
                session.sessionId to state.getTotalRequestedQuantity()
            }.sumOf { it.second }
            val currentTime = LocalDateTime.now()
            val sessions = state.map { (session, state) ->
                toSessionDetails(session, state, currentTime)
            }
            DroidherdClientStatus(
                quotaService.get(clientId),
                totalRequested,
                sessions
            )
        } else DroidherdClientStatus(
            quotaService.get(clientId),
            0,
            emptyList()
        )

    override fun getGlobalStatus() = DroidherdStatus(
        kubeService.getState().getAllResourcesByClient().map { (client, stateBySession) ->
            client to stateBySession.map { (session, state) ->
                session.sessionId to state.getReadyEmulators().size
            }.toMap()
        }.toMap()
    )

    override fun requestEmulators(request: EmulatorsRequestData): List<EmulatorRequest> {
        val clientAttributes = request.data.clientAttributes
        log.info(
            "request from {} for emulators. requests: {}, parameters: {}, client attributes: {}. client ip: {}, client debug: {}",
            kv("session", request.session),
            request.data.requests,
            request.data.parameters,
            kv("clientAttributes", clientAttributes),
            kv("clientIp", request.clientIp),
            kv("clientDebug", request.data.debug)
        )
        val kubeState = kubeService.getState()
        if (kubeState.containsSession(request.session)) {
            return kubeState.get(request.session).getRequests()
        }

        runCatching {
            validateRequestParameters(request)
        }.exceptionOrNull()?.let {
            throw IllegalArgumentException(it.message, it)
        }

        request.data.requests.forEach {
            emulatorsRequestedCounter.labels(request.session.clientId, it.image)
                .inc(it.quantity.toDouble())
        }
        val validRequests = validateQuotaInRequests(request, kubeState)
        val totalQuantity = validRequests.sumOf { it.quantity }
        if (totalQuantity > 0) {
            kubeService.createSession(
                request.copy(
                    data = SessionRequest(
                        request.data.clientAttributes,
                        validRequests,
                        request.data.parameters,
                        request.data.debug
                    )
                )
            )
        }
        return validRequests
    }

    private fun validateQuotaInRequests(request: EmulatorsRequestData, kubeState: ReadOnlyKubeState): List<EmulatorRequest> {
        val quotaResult = quotaService.validate(
            request.session, request.data.requests, kubeState.get(request.session.clientId)
        )
        return quotaResult.requests
    }

    override fun release(session: Session) {
        release(session, false)
    }

    private fun release(session: Session, invalidation: Boolean) {
        log.info("Releasing {}", session)
        val resource = kubeService.getState().get(session)
        if (!invalidation &&
            resource.getTotalRequestedQuantity() > 0
            && resource.getReadyEmulators().isEmpty()
            && resource.getCrd().metadata?.deletionTimestamp == null) {
            log.error("Session {} hasn't probably been created - all emulators not ready", session)
            emptySessionsTotal.labels(session.clientId).inc()
        }
        if (!kubeService.deleteCrd(session)) {
            throw RuntimeException("Unable to release session $session")
        }
    }

    override fun releaseAll(clientId: String) {
        val sessions = kubeService.getState().get(clientId).keys
        sessions.forEach { release(it, true) }
    }

    override fun dumpState(): DroidherdSystemStatus =
        DroidherdSystemStatus(
            kubeService.getState().getAllResources()
                .associateBy { it.getSession() }
        )

    override fun ping(session: Session) {
        kubeService.getState().let {
            if (it.containsSession(session)) {
                kubeService.updateSessionLastSeen(it.get(session), LocalDateTime.now())
            }
        }
    }

    private fun toSessionDetails(session: Session, resource: DroidherdResource, currentTime: LocalDateTime) =
        DroidherdSessionDetails(
            session.sessionId,
            resource.getTotalRequestedQuantity(),
            dateTimeFormatter.format(resource.getCreatedAt()),
            Duration.between(resource.getCreatedAt(), currentTime).seconds,
            resource.getReadyEmulators(),
            resource.getCi()
        )

    override fun invalidateResources() {
        log.debug("Invalidating resources")
        val resources = kubeService.getAllActualResources()
        val currentTime = LocalDateTime.now()
        val limitLastSeen = currentTime.minusSeconds(config.lastSeenMaxDeltaSeconds)
        val limitCreatedAt = currentTime.minusSeconds(config.sessionValidationExpiredAfterSeconds)
        val invalid = resources.count { resource ->
            val isValidResource = isValidResource(resource, limitLastSeen, limitCreatedAt)
            if (!isValidResource) {
                sessionsExpiredTotal.labels(resource.getSession().clientId).inc()
                try {
                    release(resource.getSession(), true)
                } catch (e: RuntimeException) {
                    log.error("Error during invalidating session: {}", resource.getSession(), e)
                }
            }
            !isValidResource
        }
        if (invalid > 0) {
            log.info("Invalidation finished. Total: {} sessions ({} were invalidated)", resources.size - invalid, invalid)
        }
    }

    override fun postEmulatorStartupMetrics(metric: EmulatorStartupMetric) {
        emulatorStartupTime.labels(metric.image).set(metric.value)
        log.info("Got emulator startup metrics: {}", metric)
    }

    override fun postMetrics(session: Session, metrics: List<DroidherdClientMetric>) {
        val mapMetrics = metrics.associate { it.key to it.value }
        log.info("Metrics published for {}: {}", kv("session", session), kv("metrics", mapMetrics))
    }

    private fun isValidResource(resource: DroidherdResource, limitLastSeen: LocalDateTime, limitCreatedAt: LocalDateTime): Boolean {
        if (!resource.isDebugSession() && resource.getLastSeen().isBefore(limitLastSeen)) {
            log.warn("{} hasn't been seen since {}, while limit = {}, invalidate state",
                resource.getSession(), resource.getLastSeen(), limitLastSeen)
            return false
        }
        if (resource.getCreatedAt().isBefore(limitCreatedAt)) {
            log.warn("{} is expired (created at {}, while limit = {}), invalidate state",
                resource.getSession(), resource.getCreatedAt(), limitCreatedAt)
            return false
        }
        return true
    }

    private fun validateRequestParameters(request: EmulatorsRequestData) {
        request.data.requests.forEach {
            if (!config.allowedImages.contains(it.image)) {
                throw IllegalArgumentException("Image ${it.image} is not allowed for usage. You can get allowed images using endpoint: /api/public/allowed-images")
            }
        }
        validateString(1, MAX_K8S_LABEL_LENGTH, request.session.clientId, "clientId")
        validateString(8, MAX_SESSION_ID_LENGTH, request.session.sessionId, "sessionId", regex = ALPHANUM_REGEX_WITHOUT_HYPHEN)
        with(request.data.clientAttributes) {
            validateString(1, MAX_CLIENT_VERSION_LENGTH, version, "client.version", false)
            validateString(1, MAX_CLIENT_INFO_LENGTH, info, "client.info", false)
            ci?.let {
                validateString(0, MAX_K8S_LABEL_LENGTH, it.name, "ci.name")
                validateString(0, MAX_K8S_LABEL_LENGTH, it.reference, "ci.reference", false)
                validateString(0, MAX_K8S_LABEL_LENGTH, it.repository, "ci.repository", false)
                validateString(0, MAX_K8S_LABEL_LENGTH, it.triggeredBy, "ci.name", false)
                validateString(0, MAX_CI_URL_LENGTH, it.jobUrl, "ci.name", false)
            }
            metadata?.let {
                it.forEach {entry ->
                    validateString(1, MAX_K8S_LABEL_LENGTH, entry.key, "metadata.${entry.key}")
                    validateString(1, MAX_K8S_LABEL_LENGTH, entry.value, "metadata.${entry.key}=${entry.value}")
                }
            }
        }
        request.data.parameters?.let { parameters ->
            parameters.forEach {
                validateString(1, MAX_PARAMETER_NAME_LENGTH, it.name, "parameter.${it.name}")
                validateString(0, MAX_PARAMETER_LENGTH, it.value, "parameter.${it.name}=${it.value}", false)
            }
        }
    }

    private fun validateString(min: Int, max: Int, value: String, name: String, checkAlphaNum: Boolean = true, regex: Regex = ALPHANUM_REGEX) {
        if (value.length < min) {
            throw IllegalArgumentException("Illegal $name length, less than $min: [$value]")
        }
        if (value.length > max) {
            throw IllegalArgumentException("Illegal $name length, more than $max: [$value]")
        }
        if (checkAlphaNum && !regex.matches(value)) {
            throw IllegalArgumentException("Illegal $name value, it is contain illegal characters: [$value], regex pattern: [${regex.pattern}]")
        }
    }

    override fun getAllowedImages(): Collection<String> = config.allowedImages.keys
}
