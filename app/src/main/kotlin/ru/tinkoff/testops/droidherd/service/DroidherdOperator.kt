package ru.tinkoff.testops.droidherd.service

import io.kubernetes.client.extended.controller.reconciler.Request
import io.kubernetes.client.extended.controller.reconciler.Result
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import ru.tinkoff.testops.droidherd.api.EmulatorRequest
import ru.tinkoff.testops.droidherd.models.V1DroidherdSessionStatusEmulators
import ru.tinkoff.testops.droidherd.service.configs.DroidherdConfig
import ru.tinkoff.testops.droidherd.service.kubernetes.KubeService
import ru.tinkoff.testops.droidherd.service.kubernetes.TemplateParameters
import ru.tinkoff.testops.droidherd.service.models.DroidherdResource
import ru.tinkoff.testops.droidherd.service.models.ReconcileResult
import ru.tinkoff.testops.droidherd.service.models.Session
import ru.tinkoff.testops.droidherd.service.quota.QuotaService
import java.time.Duration

class DroidherdOperator(
    private val kubeService: KubeService,
    private val quotaService: QuotaService,
    private val config: DroidherdConfig,
    registry: CollectorRegistry = CollectorRegistry.defaultRegistry
) {
    companion object {
        private val RESULT_OK = Result(false)
    }

    private val log = LoggerFactory.getLogger(javaClass)

    private val resultRequeueAfterDefault = Result(true, Duration.ofSeconds(config.requeueAfterDefaultSeconds))
    private val resultRequeueAfterPending = Result(true, Duration.ofSeconds(config.requeueAfterPendingSeconds))
    private val resultRequeueAfterCreation = Result(true, Duration.ofSeconds(config.requeueAfterCreationSeconds))

    private val apiCallsChannel: Channel<() -> Unit> = Channel()

    private val emulatorsAllocationFailedTotal = Counter.build()
        .name("emulators_allocation_failed_total")
        .help("Number of emulators creation failures per client/image over time")
        .labelNames("client", "image")
        .register(registry)

    private val scope = CoroutineScope(
        newFixedThreadPoolContext(config.apiCallWorkersCount, "operator-api-call-worker")
    )

    fun init() {
        GlobalScope.launch(Dispatchers.IO) {
            for (request in apiCallsChannel) {
                scope.launch {
                    runCatching {
                        request.invoke()
                    }.onFailure { log.error("Failed to make api call", it) }
                }
            }
        }
    }

    fun reconcileSession(request: Request): Result {
        return runCatching {
            log.debug("Reconciling session {}", request)
            process(request)
        }.onFailure {
            log.error("Exception occurred during processing {}", request, it)
        }.onSuccess {
            log.info("Reconcile completed for {}: {}, {} {}", request, it.status, it.result, it.details)
        }.getOrElse {
            ReconcileResult(resultRequeueAfterDefault, ReconcileResult.Status.Error)
        }.result
    }

    private fun process(request: Request): ReconcileResult {
        val droidherdSession = kubeService.getCrd(request.name)

        return if (!droidherdSession.isExist()) {
            deleteSession(request)
        } else {
            reconcileSession(droidherdSession)
        }
    }

    private fun deleteSession(request: Request): ReconcileResult {
        kubeService.deleteSessionFromState(getSession(request.name))
        return ReconcileResult(RESULT_OK, ReconcileResult.Status.Deleted)
    }

    private fun reconcileSession(resource: DroidherdResource): ReconcileResult {
        if (!validateQuota(resource)) {
            return ReconcileResult(RESULT_OK, ReconcileResult.Status.QuotaPatched)
        }

        val runningEmulators = kubeService.getEmulators(resource.getSession())
        if ((resource.getReadyEmulators().size == resource.getTotalRequestedQuantity())
            && (runningEmulators.size == resource.getTotalRequestedQuantity())
        ) {
            kubeService.updateState(resource)
            return ReconcileResult(RESULT_OK, ReconcileResult.Status.Reconciled)
        }

        val emulatorsFromStatus = resource.getEmulatorsFromStatus()

        if (runningEmulators.size > resource.getTotalRequestedQuantity()) {
            reduceEmulators(resource, runningEmulators)
            return ReconcileResult(resultRequeueAfterDefault, ReconcileResult.Status.Reducing)
        }

        if (isResourceStatusUpdateNeeded(emulatorsFromStatus, runningEmulators)) {
            kubeService.updateSessionEmulators(resource, runningEmulators)
            return ReconcileResult(resultRequeueAfterPending, ReconcileResult.Status.StatusUpdated, generateSessionDetails(runningEmulators, resource))
        }

        if (emulatorsFromStatus.size < resource.getTotalRequestedQuantity()) {
            createEmulators(resource, runningEmulators.map { it.id }.toSet())
            return ReconcileResult(resultRequeueAfterCreation, ReconcileResult.Status.Creating)
        }

        return ReconcileResult(resultRequeueAfterPending, ReconcileResult.Status.Pending, generateSessionDetails(runningEmulators, resource))
    }

    private fun generateSessionDetails(runningEmulators: List<V1DroidherdSessionStatusEmulators>, resource: DroidherdResource): String {
        val readyEmulators = runningEmulators.count { it.ready }
        return "ready $readyEmulators from ${runningEmulators.size}, requested: ${resource.getTotalRequestedQuantity()}" +
                ", version ${resource.getCrd().metadata?.resourceVersion}"
    }

    private fun isResourceStatusUpdateNeeded(
        runningEmulators: List<V1DroidherdSessionStatusEmulators>,
        emulatorsInStatus: List<V1DroidherdSessionStatusEmulators>
    ): Boolean {
        val isStatusSame = (emulatorsInStatus.size == runningEmulators.size)
            && emulatorsInStatus.containsAll(runningEmulators)
        return !isStatusSame
    }

    private fun createEmulators(resource: DroidherdResource, runningEmulatorsIds: Set<String>) {
        resource.getRequests().forEach { request ->
            for (i in 0 until request.quantity) {
                val name = generateEmulatorName(resource.getSession().sessionId, request.image, i)
                if (!runningEmulatorsIds.contains(name)) {
                    submitCreationApiCall(resource, request, name)
                }
            }
        }
    }

    private fun submitCreationApiCall(resource: DroidherdResource, request: EmulatorRequest, name: String) {
        enqueueApiCall {
            val templateParameters = TemplateParameters(name, resource, request.image, config)
            log.info("Creating emulator {} for {}", name, resource.getSession())
            try {
                kubeService.createEmulator(templateParameters)
            } catch (e: Exception) {
                log.error("Failed to allocate emulator {} for {}", name, resource.getSession(), e)
                emulatorsAllocationFailedTotal.labels(resource.getSession().clientId, request.image).inc()
            }
        }
    }

    private fun validateQuota(resource: DroidherdResource): Boolean {
        val sessionsById = mutableMapOf<Session, DroidherdResource>().also {
            it.putAll(kubeService.getState().get(resource.getSession().clientId))
            it[resource.getSession()] = resource
        }
        val quotaResult = quotaService.validate(
            resource.getSession(), resource.getRequests(), sessionsById
        )
        if (quotaResult.isValid) {
            return true
        }

        val sessionsToPatch = quotaService.validateBySessionsAge(sessionsById).also {
            if (it.isEmpty()) {
                log.warn(
                    "Quota validation failed, result is {}" +
                    " but after validate by age results is empty. Sessions by id: {}, resource: {}",
                    quotaResult, sessionsById, resource
                )
            }
        }
        sessionsToPatch.forEach {
            if (it.getTotalRequestedQuantity() == 0) {
                log.warn("Session {} completely exceeded quota: request will be released", it.getSession())
                kubeService.deleteCrd(it.getSession())
            } else {
                log.warn("Requests of {} will be updated: {}", it.getSession(), it.getRequests())
                kubeService.updateCrd(it)
            }
        }
        return false
    }

    private fun generateEmulatorName(id: String, image: String, seq: Int) = "droid-$id-$image-$seq"

    private fun getSession(crdName: String): Session {
        val clientId = crdName.substringBeforeLast('-')
        val sessionId = crdName.substringAfterLast('-')

        return Session(clientId, sessionId)
    }

    private fun reduceEmulators(resource: DroidherdResource, runningEmulators: List<V1DroidherdSessionStatusEmulators>) {
        val quantityByImage = getQuantityByImage(resource.getRequests())
        val ids = runningEmulators.mapNotNull { emulator ->
            val seq = getEmulatorSequentialNumber(emulator.id)
            quantityByImage[emulator.image]?.let {
                if (seq >= it) emulator.id else null
            }
        }
        if (ids.isNotEmpty()) {
            enqueueApiCall {
                log.info("Deleting emulators with ids {}", ids)
                kubeService.deleteEmulatorsWithIds(ids)
            }
        }
    }

    private fun enqueueApiCall(func: () -> Unit) = runBlocking { apiCallsChannel.send(func) }

    private fun getEmulatorSequentialNumber(id: String): Int = id.split('-').last().toInt()

    private fun getQuantityByImage(requests: List<EmulatorRequest>): Map<String, Int> =
        requests.groupingBy { it.image }.fold(0) { a: Int, e: EmulatorRequest -> a + e.quantity }
}
