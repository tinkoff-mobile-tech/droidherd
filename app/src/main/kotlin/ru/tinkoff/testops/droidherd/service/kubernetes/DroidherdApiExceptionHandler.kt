package ru.tinkoff.testops.droidherd.service.kubernetes

import io.kubernetes.client.openapi.ApiException
import org.slf4j.LoggerFactory
import ru.tinkoff.testops.droidherd.models.V1DroidherdSession
import ru.tinkoff.testops.droidherd.service.ShutdownManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer

class DroidherdApiExceptionHandler(
    private val shutdownManager: ShutdownManager
) : BiConsumer<Class<V1DroidherdSession>?, Throwable?> {

    companion object {
        private const val MAX_4xx_EXCEPTIONS_PER_MINUTE = 5
    }

    private val log = LoggerFactory.getLogger(javaClass)

    private val fatalExceptionsCounter = AtomicInteger(0)
    @Volatile
    private var lastExceptionDateTime: LocalDateTime = LocalDateTime.MIN

    override fun accept(type: Class<V1DroidherdSession>?, throwable: Throwable?) {
        log.error("DroidherdSessionApi exception occurred", throwable)

        if (throwable is ApiException) {
            if (throwable.code in 400..499) {
                val now = LocalDateTime.now()
                if (Duration.between(lastExceptionDateTime, now).toSeconds() > 60) {
                    fatalExceptionsCounter.set(0)
                }
                lastExceptionDateTime = now
                val exceptionsCountForLastMinute = fatalExceptionsCounter.incrementAndGet()
                if (exceptionsCountForLastMinute > MAX_4xx_EXCEPTIONS_PER_MINUTE) {
                    log.error("Something goes wrong - k8s api returns too much 4xx errors. Restart required.")
                    shutdownManager.shutdown(throwable.code)
                }
            }
        }
    }
}
