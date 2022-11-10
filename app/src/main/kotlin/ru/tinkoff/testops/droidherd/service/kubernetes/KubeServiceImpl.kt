package ru.tinkoff.testops.droidherd.service.kubernetes

import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.V1Service
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import ru.tinkoff.testops.droidherd.models.V1DroidherdSession
import ru.tinkoff.testops.droidherd.models.V1DroidherdSessionStatus
import ru.tinkoff.testops.droidherd.models.V1DroidherdSessionStatusEmulators
import ru.tinkoff.testops.droidherd.models.V1DroidherdSessionStatusExtraUris
import ru.tinkoff.testops.droidherd.service.models.DroidherdResource
import ru.tinkoff.testops.droidherd.service.models.EmulatorsRequestData
import ru.tinkoff.testops.droidherd.service.models.Session
import java.time.LocalDateTime

class KubeServiceImpl(
    private val kubeClient: KubeClient
) : KubeService {

    @Volatile
    private var state = KubeState(kubeClient.getAllSessionsByClientId())
    override fun getCrd(name: String): DroidherdResource {
        val session = kubeClient.getResource(name)
        return session?.let {
            DroidherdResource(it)
        } ?: DroidherdResource.NULL_RESOURCE
    }

    private val mutex = Mutex()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun getState(): ReadOnlyKubeState = state

    override fun createSession(request: EmulatorsRequestData): DroidherdResource {
        val droidherdSession = kubeClient.create(request)
        updateState(droidherdSession)
        return droidherdSession
    }

    override fun updateCrd(resource: DroidherdResource): Boolean {
        return runCatching {
            val updatedResource = DroidherdResource(
                kubeClient.updateSession(resource.getCrd())
            )
            updateState(updatedResource)
            true
        }.onFailure {
            log.error("Failed to update resource: {}, {}", resource.getName(), resource.getUid(), it)
        }.getOrElse { false }
    }

    override fun updateState(resource: DroidherdResource) {
        runBlocking {
            mutex.withLock {
                state = state.add(resource)
            }
        }
    }

    override fun getEmulators(session: Session): List<V1DroidherdSessionStatusEmulators> {
        val pods = kubeClient.getPods(session)
        val services = kubeClient.getServices(session)
        return pods.mapNotNull {
            val id = it.value.metadata?.name ?: ""
            mapEmulatorOrNull(id, it.value, services)
        }
    }

    private fun mapEmulatorOrNull(
        droidherdId: String,
        pod: V1Pod,
        services: Map<String, V1Service>
    )
        : V1DroidherdSessionStatusEmulators? {
        val nullableService = services[droidherdId]
        if (nullableService == null) {
            log.warn("No service or pod with id: {}. Pod name: {}", droidherdId, pod.metadata?.name)
            return null
        }
        val service: V1Service = nullableService

        val podImage = pod.metadata?.labels?.get("image")
        if (podImage == null) {
            log.warn("Emulator pod {} doesn't have image label. Can't map to emulator.", droidherdId)
            return null
        }
        val hostIp = pod.status?.hostIP
        if (hostIp == null) {
            log.warn("Pod {} doesn't have hostIp. Can't map to emulator.", droidherdId)
            return null
        }

        return V1DroidherdSessionStatusEmulators().apply {
            id = pod.metadata?.name
            ready = (pod.status?.conditions?.find { it.type == "Ready" }?.status ?: "False") == "True"
            image = podImage
            adb = "${hostIp}:${service.spec?.ports?.get(0)?.nodePort}"

            val extraPorts = service.spec?.ports?.drop(1) ?: listOf()
            extraUris = extraPorts.map { port ->
                V1DroidherdSessionStatusExtraUris().apply {
                    name = port.name
                    uri = "${hostIp}:${port.nodePort}"
                }
            }
        }
    }

    override fun createEmulator(templateParameters: TemplateParameters) {
        create("service") {
            kubeClient.createService(templateParameters)
        }
        create("pod") {
            kubeClient.createPod(templateParameters)
        }
    }

    override fun deleteEmulatorsWithIds(ids: List<String>) {
        val joinedIds = ids.joinToString(separator = ", ", prefix = "(", postfix = ")")
        val label = "droidherdId in $joinedIds"

        delete("pod") {
            kubeClient.deletePodsWithLabel(label)
        }
        delete("service") {
            kubeClient.deleteServicesWithLabel(label)
        }
    }

    override fun deleteSessionFromState(session: Session) {
        if (state.containsSession(session)) {
            runBlocking {
                mutex.withLock {
                    state = state.delete(session)
                }
            }
        }
    }

    override fun deleteCrd(session: Session): Boolean {
        val deleted = kubeClient.delete(session)
        if (deleted) {
            deleteSessionFromState(session)
        }
        return deleted
    }

    override fun updateSessionLastSeen(resource: DroidherdResource, time: LocalDateTime) {
        updateStatus(resource) {
            val status = it.status ?: V1DroidherdSessionStatus()
            status.lastSeen(time.toString())
        }
    }

    override fun updateSessionEmulators(resource: DroidherdResource, emulators: List<V1DroidherdSessionStatusEmulators>) {
        updateStatus(resource) {
            val status = it.status ?: V1DroidherdSessionStatus()
            status.emulators(emulators)
        }
    }

    private fun updateStatus(resource: DroidherdResource, status: (V1DroidherdSession) -> V1DroidherdSessionStatus) {
        val session = resource.getSession()
        if (!resource.isExist()) {
            throw RuntimeException("CRD for session $session not exist during update status")
        }

        val response = kubeClient.updateStatus(resource.getCrd(), status).throwsApiException()
        if (!response.isSuccess) {
            log.error("Unable to update status for {}: {}, {}", session, response.httpStatusCode, response.status)
            throw RuntimeException("Unable to update status for $session")
        }
        updateState(DroidherdResource(response.getObject()))
    }

    private fun create(resourceName: String, create: () -> Unit) {
        try {
            create.invoke()
        } catch (e: ApiException) {
            val code = e.code
            if (code != 409) { // already exists
                log.error("Error with unknown code {} and details {} on creating the {}", e.code, e.message, resourceName, e)
                throw e
            }
        }
    }

    private fun delete(resourceName: String, delete: () -> Unit) {
        try {
            delete.invoke()
        } catch (e: ApiException) {
            val code = e.code
            if (code != 404) { // not found
                log.error("Error with unknown code {} and details {} on deleting the {}", e.code, e.message, resourceName, e)
                throw e
            }
        }
    }
}
