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

    private val resultRequeueAfterTimeout = Result(true, Duration.ofSeconds(config.requeueAfterSeconds))

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

    fun reconcilePod(request: Request): Result {
        log.debug("reconciling pod {}", request)
        // we actually need only indexer for client to get actual state of pods
        // so didn't perform any actions here
        return RESULT_OK
    }

    fun reconcileService(request: Request): Result {
        log.debug("reconciling service {}", request)
        // we actually need only indexer for client to get actual state of services
        // so didn't perform any actions here
        return RESULT_OK
    }

    fun reconcileSession(request: Request): Result {
        return runCatching {
            log.info("Reconciling session $request")
            process(request)
        }.onFailure {
            log.error("Exception occurred during processing $request", it)
        }.onSuccess {
            log.info("Reconcile completed for ${request}: ${it.status}, ${it.result}")
        }.getOrElse {
            ReconcileResult(resultRequeueAfterTimeout, ReconcileResult.Status.Error)
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

    private fun updateState(resource: DroidherdResource): ReconcileResult {
        kubeService.updateState(resource)
        return ReconcileResult(RESULT_OK, ReconcileResult.Status.StatusUpdated)
    }

    private fun deleteSession(request: Request): ReconcileResult {
        kubeService.deleteSessionFromState(getSession(request.name))
        return ReconcileResult(RESULT_OK, ReconcileResult.Status.Deleted)
    }

    private fun reconcileSession(resource: DroidherdResource): ReconcileResult {
        if (!validateQuota(resource)) {
            return ReconcileResult(RESULT_OK, ReconcileResult.Status.QuotaPatched)
        }

        updateState(resource)
        val runningEmulators = kubeService.getEmulators(resource.getSession())
        if ((resource.getReadyEmulators().size == resource.getTotalRequestedQuantity())
            && (runningEmulators.size == resource.getTotalRequestedQuantity())
        ) {
            return ReconcileResult(RESULT_OK, ReconcileResult.Status.Reconciled)
        }

        val emulatorsFromStatus = resource.getEmulatorsFromStatus()
        if (isResourceStatusUpdateNeeded(emulatorsFromStatus, runningEmulators)) {
            kubeService.updateSessionEmulators(resource.getSession(), runningEmulators)
            return ReconcileResult(RESULT_OK, ReconcileResult.Status.StatusUpdated)
        }

        if (runningEmulators.size > resource.getTotalRequestedQuantity()) {
            reduceEmulators(resource, runningEmulators)
            return ReconcileResult(resultRequeueAfterTimeout, ReconcileResult.Status.Reducing)
        }

        if (emulatorsFromStatus.size < resource.getTotalRequestedQuantity()) {
            createEmulators(resource, runningEmulators.map { it.id }.toSet())
            return ReconcileResult(resultRequeueAfterTimeout, ReconcileResult.Status.Creating)
        }

        return ReconcileResult(resultRequeueAfterTimeout, ReconcileResult.Status.Pending)
    }

    private fun isResourceStatusUpdateNeeded(
        runningEmulators: List<V1DroidherdSessionStatusEmulators>,
        emulatorsInStatus: List<V1DroidherdSessionStatusEmulators>
    ): Boolean {
        val isStatusSame = (emulatorsInStatus.size == runningEmulators.size)
            && emulatorsInStatus.containsAll(runningEmulators)
        if (isStatusSame) {
            return false
        }
        return true
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
            log.info("Creating emulator $name for ${resource.getSession()}")
            try {
                kubeService.createEmulator(resource, templateParameters)
            } catch (e: RuntimeException) {
                log.error("Failed to allocate emulator $name for ${resource.getSession()}", e)
                emulatorsAllocationFailedTotal.labels(resource.getSession().clientId, request.image).inc()
            }
        }
    }

    private fun validateQuota(resource: DroidherdResource): Boolean {
        val sessionsById = kubeService.getState().get(resource.getSession().clientId)
        val quotaResult = quotaService.validate(
            resource.getSession(), resource.getRequests(), sessionsById
        )
        if (quotaResult.isValid) {
            return true
        }

        val sessionsToPatch = quotaService.validateBySessionsAge(sessionsById).also {
            if (it.isEmpty()) {
                log.error(
                    "Quota validation failed for ${resource.getSession()} result is $quotaResult" +
                        " but after validate by age results is empty."
                )
            }
        }
        sessionsToPatch.forEach {
            if (it.getTotalRequestedQuantity() == 0) {
                log.warn("Session ${it.getSession()} completely exceeded quota: request will be released")
                kubeService.deleteCrd(it.getSession())
            } else {
                log.warn("Requests of ${it.getSession()} will be updated: ${it.getRequests()}")
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
                log.info("Deleting emulators with ids $ids")
                kubeService.deleteEmulatorsWithIds(ids)
            }
        }
    }

    private fun enqueueApiCall(func: () -> Unit) = runBlocking { apiCallsChannel.send(func) }

    private fun getEmulatorSequentialNumber(id: String): Int = id.split('-').last().toInt()

    private fun getQuantityByImage(requests: List<EmulatorRequest>): Map<String, Int> =
        requests.groupingBy { it.image }.fold(0) { a: Int, e: EmulatorRequest -> a + e.quantity }
}
