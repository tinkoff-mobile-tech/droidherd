package ru.tinkoff.testops.droidherd.service.configs

data class DroidherdConfig(
    val namespace: String,
    val sessionValidationExpiredAfterSeconds: Long,
    val lastSeenMaxDeltaSeconds: Long,
    val invalidateSessionsPeriodSeconds: Long,
    val operatorResyncPeriodMinutes: Long,
    val listResourcesTimeoutSeconds: Int,
    val superuser: String,
    val crdTemplatePath: String,
    val podTemplatePath: String,
    val serviceTemplatePath: String,
    val allowedImages: Map<String, String>,
    val requeueAfterDefaultSeconds: Long,
    val requeueAfterPendingSeconds: Long,
    val requeueAfterCreationSeconds: Long,
    val dryRun: Boolean,
    val applyCrdAtStartup: Boolean,
    val droidherdHost: String,
    val pretty: String = "true",
    val fieldManager: String = "",
    val fieldValidation: String = "",
    val gracePeriodSeconds: Int? = null,
    val orphanDependents: Boolean? = null,
    val propagationPolicy: String? = null,
    val apiCallWorkersCount: Int = 4
)
