package ru.tinkoff.testops.droidherd.service.kubernetes

import com.hubspot.jinjava.Jinjava
import io.kubernetes.client.extended.kubectl.Kubectl
import io.kubernetes.client.informer.SharedIndexInformer
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.*
import io.kubernetes.client.util.Yaml
import io.kubernetes.client.util.generic.KubernetesApiResponse
import io.kubernetes.client.util.generic.options.CreateOptions
import io.kubernetes.client.util.generic.options.ListOptions
import org.slf4j.LoggerFactory
import ru.tinkoff.testops.droidherd.DroidherdCrdFileProvider
import ru.tinkoff.testops.droidherd.api.*
import ru.tinkoff.testops.droidherd.models.V1DroidherdSession
import ru.tinkoff.testops.droidherd.models.V1DroidherdSessionSpecEmulators
import ru.tinkoff.testops.droidherd.models.V1DroidherdSessionSpecParameters
import ru.tinkoff.testops.droidherd.models.V1DroidherdSessionStatus
import ru.tinkoff.testops.droidherd.service.configs.DroidherdConfig
import ru.tinkoff.testops.droidherd.service.models.DroidherdResource
import ru.tinkoff.testops.droidherd.service.models.EmulatorsRequestData
import ru.tinkoff.testops.droidherd.service.models.Session
import java.io.File

class KubeClient(
    private val config: DroidherdConfig,
    private val droidherdSessionApi: V1DroidherdSessionApi,
    private val coreApi: CoreV1Api,
    private val sessionIndexInformer: SharedIndexInformer<V1DroidherdSession>,
    private val podIndexInformer: SharedIndexInformer<V1Pod>,
    private val serviceIndexInformer: SharedIndexInformer<V1Service>,
    private val crdFileProvider: DroidherdCrdFileProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val yamlRenderer = Jinjava()

    private val crdYaml = readFile(config.crdTemplatePath)
    private val podYaml = readFile(config.podTemplatePath)
    private val serviceYaml = readFile(config.serviceTemplatePath)

    private fun readFile(path: String): String {
        return if (path.startsWith("classpath:"))
            javaClass.getResource(path.substringAfter("classpath:"))?.readText()
                ?: throw RuntimeException("Failed to read config: $path")
        else File(path).readText()
    }

    fun dryRun() {
        try {
            log.info("Creating droidherdsession with dry run to check environment")
            val request = EmulatorsRequestData(
                Session("droidherd-operator", "dry-run-test"),
                "127.0.0.1",
                SessionRequest(
                    ClientAttributes("dry-run", "dry-run-test", CiAttributes.EMPTY, emptyMap()),
                    listOf(EmulatorRequest("stub", 1)),
                    listOf(EmulatorParameter("name", "value")),
                    false
                )
            )
            create(request, "All")
            log.info("Dry run creation of droidherdsession passed")
        } catch (e: Exception) {
            throw RuntimeException(
                "Dry run request for DroidherdSession creation failed. Seems k8s unavailable or not configured properly",
                e
            )
        }
    }

    fun applyCrd(config: DroidherdConfig, apiClient: ApiClient) {
        val crdFile = crdFileProvider.provide()
        val crd = Yaml.loadAs(crdFile, V1CustomResourceDefinition::class.java)
        Kubectl.apply(V1CustomResourceDefinition::class.java)
            .fieldManager("java-kubectl")
            .forceConflict(true)
            .apiClient(apiClient)
            .resource(crd)
            .namespace(config.namespace)
            .execute()
    }

    fun create(request: EmulatorsRequestData) = create(request, null)

    private fun create(request: EmulatorsRequestData, dryRunOption: String?): DroidherdResource {
        val parameters = generateCrdParameters(request)
        val session = loadYamlAs(crdYaml, parameters, V1DroidherdSession::class.java)
        session.metadata?.namespace = config.namespace
        if (!request.data.clientAttributes?.metadata.isNullOrEmpty()) {
            val labels = session.metadata?.labels ?: mutableMapOf()
            labels.plus(request.data.clientAttributes.metadata)
            session.metadata?.labels = labels
        }
        session.spec?.parameters = request.data.parameters.map {
            V1DroidherdSessionSpecParameters().apply {
                name = it.name
                value = it.value
            }
        }
        session.spec?.emulators = request.data.requests.map {
            V1DroidherdSessionSpecEmulators()
                .apply {
                    image = it.image
                    quantity = it.quantity
                }
        }

        log.debug("creating crd (dryRun={}): {}", dryRunOption, session)

        return DroidherdResource(
            droidherdSessionApi.create(
                session, CreateOptions().apply {
                    dryRun = dryRunOption
                }
            ).throwsApiException().getObject()
        )
    }

    private fun generateCrdParameters(request: EmulatorsRequestData): Map<String, Any> {
        val parameters = mutableMapOf(
            "Name" to generateResourceName(request.session),
            "ClientId" to request.session.clientId,
            "SessionId" to request.session.sessionId,
            "ClientIp" to request.clientIp,
            "ClientVersion" to request.data.clientAttributes.version,
            "ClientInfo" to request.data.clientAttributes.info,
            "IsDebug" to request.data.debug.toString()
        )
        request.data.clientAttributes.ci?.let {
            parameters.putAll(
                mapOf(
                    "ciName" to it.name,
                    "ciRepository" to it.repository,
                    "ciReference" to it.reference,
                    "ciJobUrl" to it.jobUrl,
                    "ciTriggeredBy" to it.triggeredBy,
                )
            )
        }
        return parameters
    }

    fun delete(session: Session): Boolean {
        val response = droidherdSessionApi.delete(config.namespace, generateResourceName(session))
        val success = response.isSuccess || response.status.code == 404
        if (!success) {
            log.error("Unable to release session {}: {}", session, response.status)
        }
        return success
    }

    fun getAllSessionsByClientId(): Map<String, Map<Session, DroidherdResource>> {
        return droidherdSessionApi
            .list(config.namespace, ListOptions().apply {
                timeoutSeconds = config.listResourcesTimeoutSeconds
            }
            ).throwsApiException()
            .getObject().items.map { DroidherdResource(it) }
            .groupBy({ it.getSession().clientId },
                { it.getSession() to it })
            .mapValues { (_, values) -> values.toMap() }
    }

    fun updateStatus(
        session: V1DroidherdSession,
        status: (V1DroidherdSession) -> V1DroidherdSessionStatus
    ): KubernetesApiResponse<V1DroidherdSession> =
        droidherdSessionApi.updateStatus(session, status)

    fun getResource(name: String): V1DroidherdSession? =
        sessionIndexInformer.indexer.getByKey("${config.namespace}/$name")

    fun updateSession(session: V1DroidherdSession): V1DroidherdSession =
        droidherdSessionApi.update(session).throwsApiException().getObject()

    fun createPod(templateParameters: TemplateParameters): V1Pod {
        val pod = loadYamlAs(podYaml, templateParameters.asMap, V1Pod::class.java)
        log.debug("creating pod {}", pod)

        return coreApi.createNamespacedPod(
            config.namespace, pod,
            config.pretty, null,
            config.fieldManager, config.fieldValidation
        )
    }

    fun createService(templateParameters: TemplateParameters): V1Service {
        val service = loadYamlAs(serviceYaml, templateParameters.asMap, V1Service::class.java)
        log.debug("creating service {}", service)
        return coreApi.createNamespacedService(
            config.namespace, service,
            config.pretty, null,
            config.fieldManager, config.fieldValidation
        )
    }

    fun deletePodsWithLabel(label: String) {
        coreApi.deleteCollectionNamespacedPod(
            config.namespace, config.pretty, null,
            null, null, config.gracePeriodSeconds,
            label, null,
            config.orphanDependents, config.propagationPolicy, null,
            null, null, null
        )
    }

    fun deleteServicesWithLabel(label: String) {
        coreApi.deleteCollectionNamespacedService(
            config.namespace, config.pretty, null,
            null, null, config.gracePeriodSeconds,
            label, null,
            config.orphanDependents, config.propagationPolicy, null,
            null, null, null
        )
    }

    fun getPods(session: Session): Map<String, V1Pod> {
        return podIndexInformer.indexer.list()
            .filter { sessionMatchedByMetadata(session, it.metadata) }
            .associateBy { it.metadata!!.name!! }
    }

    fun getServices(session: Session): Map<String, V1Service> {
        return serviceIndexInformer.indexer.list()
            .filter { sessionMatchedByMetadata(session, it.metadata) }
            .associateBy { it.metadata!!.name!! }
    }

    private fun sessionMatchedByMetadata(session: Session, metadata: V1ObjectMeta?): Boolean {
        val sessionId = metadata?.labels?.get(DroidherdReservedLabels.SESSION_ID) ?: ""
        val clientId = metadata?.labels?.get(DroidherdReservedLabels.CLIENT_ID) ?: ""
        return session.clientId == clientId && session.sessionId == sessionId
    }

    private fun <T> loadYamlAs(yaml: String, templateParameters: Map<String, Any>, clazz: Class<T>): T =
        Yaml.loadAs(yamlRenderer.render(yaml, templateParameters), clazz)

    private fun generateResourceName(session: Session) = "${session.clientId}-${session.sessionId}"

}
