package ru.tinkoff.testops.droidherd.spring

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.kubernetes.client.extended.controller.Controller
import io.kubernetes.client.extended.controller.builder.ControllerBuilder
import io.kubernetes.client.extended.controller.reconciler.Request
import io.kubernetes.client.extended.kubectl.Kubectl
import io.kubernetes.client.extended.workqueue.WorkQueue
import io.kubernetes.client.informer.SharedIndexInformer
import io.kubernetes.client.informer.SharedInformerFactory
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.*
import io.kubernetes.client.util.Yaml
import io.prometheus.client.CollectorRegistry
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.tinkoff.testops.droidherd.CRDProducer
import ru.tinkoff.testops.droidherd.models.V1DroidherdSession
import ru.tinkoff.testops.droidherd.service.DroidherdOperator
import ru.tinkoff.testops.droidherd.service.DroidherdService
import ru.tinkoff.testops.droidherd.service.DroidherdServiceImpl
import ru.tinkoff.testops.droidherd.service.ShutdownManager
import ru.tinkoff.testops.droidherd.service.configs.DroidherdConfig
import ru.tinkoff.testops.droidherd.service.configs.QuotaConfig
import ru.tinkoff.testops.droidherd.service.kubernetes.*
import ru.tinkoff.testops.droidherd.service.quota.QuotaService
import ru.tinkoff.testops.droidherd.service.quota.QuotaServiceImpl
import java.time.Duration
import java.util.concurrent.Executors
import kotlin.system.exitProcess

@Configuration
open class ApplicationConfig {
    @Bean
    open fun quotaConfig(): QuotaConfig = ConfigFactory.load().extract("quota")

    @Bean
    open fun emulatorConfig(): DroidherdConfig = ConfigFactory.load().extract("droidherd")

    @Bean
    open fun quotaService(quotaConfig: QuotaConfig): QuotaService =
        QuotaServiceImpl(quotaConfig).apply {
            init()
        }

    @Bean
    open fun shutdownManager(): ShutdownManager {
        return object : ShutdownManager {
            override fun shutdown(exitCode: Int) {
                exitProcess(exitCode)
            }
        }
    }

    @Bean
    open fun droidherdApiExceptionHandler(shutdownManager: ShutdownManager) =
        DroidherdApiExceptionHandler(shutdownManager)

    @Bean
    open fun droidherdListerWatcher(api: V1DroidherdSessionApi, config: DroidherdConfig) =
        DroidherdListerWatcher(api, config)

    @Bean
    open fun droidherdSharedIndexInformer(
        config: DroidherdConfig,
        sharedInformerFactory: SharedInformerFactory,
        listerWatcher: DroidherdListerWatcher,
        exceptionHandler: DroidherdApiExceptionHandler
    ): SharedIndexInformer<V1DroidherdSession> {
        // use custom ListerWatcher to be able setup exceptionHandler
        return sharedInformerFactory.sharedIndexInformerFor(
            listerWatcher, V1DroidherdSession::class.java, 0, exceptionHandler)
    }

    @Bean
    open fun serviceListerWatcher(api: CoreV1Api, config: DroidherdConfig):
            DroidherdServicesListerWatcher = DroidherdServicesListerWatcher(api, config)

    @Bean
    open fun servicesSharedIndexInformer(
        sharedIndexInformerFactory: SharedInformerFactory,
        listerWatcher: DroidherdServicesListerWatcher,
        config: DroidherdConfig
    ): SharedIndexInformer<V1Service> {
        return sharedIndexInformerFactory.sharedIndexInformerFor(
            listerWatcher, V1Service::class.java, Duration.ofMinutes(config.operatorResyncPeriodMinutes).toMillis()
        )
    }

    @Bean
    open fun podsListerWatcher(api: CoreV1Api, config: DroidherdConfig):
            DroidherdPodsListerWatcher = DroidherdPodsListerWatcher(api, config)

    @Bean
    open fun podsSharedIndexInformer(
        sharedIndexInformerFactory: SharedInformerFactory,
        listerWatcher: DroidherdPodsListerWatcher,
        config: DroidherdConfig
    ): SharedIndexInformer<V1Pod> {
        return sharedIndexInformerFactory.sharedIndexInformerFor(
            listerWatcher, V1Pod::class.java, Duration.ofMinutes(config.operatorResyncPeriodMinutes).toMillis()
        )
    }

    @Bean
    open fun coreV1Api(apiClient: ApiClient): CoreV1Api = CoreV1Api(apiClient)

    @Bean
    open fun droidherdSessionApi(apiClient: ApiClient) = V1DroidherdSessionApi(apiClient)

    @Bean
    open fun controller(
        config: DroidherdConfig,
        sharedInformerFactory: SharedInformerFactory,
        sessionInformer: SharedIndexInformer<V1DroidherdSession>,
        podInformer: SharedIndexInformer<V1Pod>,
        serviceInformer: SharedIndexInformer<V1Service>,
        operator: DroidherdOperator
    ): Controller {
        return ControllerBuilder
            .controllerManagerBuilder(sharedInformerFactory)
            .addController(
                ControllerBuilder.defaultBuilder(sharedInformerFactory)
                    .watch { queue: WorkQueue<Request> ->
                        ControllerBuilder
                            .controllerWatchBuilder(V1DroidherdSession::class.java, queue)
                            .withResyncPeriod(Duration.ofMinutes(config.operatorResyncPeriodMinutes))
                            .build()
                    }
                    .withWorkerCount(1)
                    .withReconciler { request -> operator.reconcileSession(request!!) }
                    .withReadyFunc { sessionInformer.hasSynced() }
                    .withName("droidherdSessionController")
                    .build()
            )
            .addController(
                ControllerBuilder.defaultBuilder(sharedInformerFactory)
                    .watch { queue: WorkQueue<Request> ->
                        ControllerBuilder
                            .controllerWatchBuilder(V1Pod::class.java, queue)
                            .withResyncPeriod(Duration.ofMinutes(config.operatorResyncPeriodMinutes))
                            .build()
                    }
                    .withWorkerCount(1)
                    .withReconciler { request -> operator.reconcilePod(request!!) }
                    .withReadyFunc { podInformer.hasSynced() }
                    .withName("podDroidherdController")
                    .build()
            ).addController(
                ControllerBuilder.defaultBuilder(sharedInformerFactory)
                    .watch { queue: WorkQueue<Request> ->
                        ControllerBuilder
                            .controllerWatchBuilder(V1Service::class.java, queue)
                            .withResyncPeriod(Duration.ofMinutes(config.operatorResyncPeriodMinutes))
                            .build()
                    }
                    .withWorkerCount(1)
                    .withReconciler { request -> operator.reconcileService(request!!) }
                    .withReadyFunc { serviceInformer.hasSynced() }
                    .withName("serviceDroidherdController")
                    .build()
            )
            .build()
    }

    @Bean
    open fun runner(sharedInformerFactory: SharedInformerFactory, controller: Controller): ApplicationRunner {
        val executorService = Executors.newSingleThreadExecutor()
        return ApplicationRunner {
            executorService.execute {
                sharedInformerFactory.startAllRegisteredInformers()
                controller.run()
            }
        }
    }

    @Bean
    open fun droidherdOperator(
        kubeClient: KubeClient,
        kubeService: KubeService,
        quotaService: QuotaService,
        config: DroidherdConfig,
        registry: CollectorRegistry
    ): DroidherdOperator = DroidherdOperator(kubeService, quotaService, config, registry).apply { init() }

    @Bean
    open fun kubeClient(
        droidherdConfig: DroidherdConfig,
        droidherdSessionApi: V1DroidherdSessionApi,
        coreV1Api: CoreV1Api,
        sessionIndexInformer: SharedIndexInformer<V1DroidherdSession>,
        podIndexInformer: SharedIndexInformer<V1Pod>,
        serviceIndexInformer: SharedIndexInformer<V1Service>,
        apiClient: ApiClient,
        crdProducer: CRDProducer
    ) = KubeClient(
        droidherdConfig, droidherdSessionApi, coreV1Api, sessionIndexInformer, podIndexInformer, serviceIndexInformer
    ).also {
        if (droidherdConfig.applyCrdAtStartup) {
            applyCrd(crdProducer, droidherdConfig, apiClient)
        }
        if (droidherdConfig.dryRun) {
            it.dryRun()
        }
    }

    @Bean
    open fun kubeService(droidherdConfig: DroidherdConfig, kubeClient: KubeClient):
            KubeService = KubeServiceImpl(kubeClient)

    @Bean
    open fun droidherdService(
        droidherdConfig: DroidherdConfig,
        kubeService: KubeService,
        quotaService: QuotaService,
        collectorRegistry: CollectorRegistry
    ): DroidherdService =
        DroidherdServiceImpl(
            droidherdConfig,
            kubeService,
            quotaService,
            collectorRegistry
        ).apply { init() }

    @Bean
    open fun crdProducer() = CRDProducer()

    private fun applyCrd(crdProducer: CRDProducer, config: DroidherdConfig, apiClient: ApiClient) {
        val crdFile = crdProducer.produceCRDFile()
        val crd = Yaml.loadAs(crdFile, V1CustomResourceDefinition::class.java)
        Kubectl.apply(V1CustomResourceDefinition::class.java)
            .fieldManager("java-kubectl")
            .forceConflict(true)
            .apiClient(apiClient)
            .resource(crd)
            .namespace(config.namespace)
            .execute()
    }
}

