package ru.tinkoff.testops.droidherd.service.configs

data class QuotaConfig(
    val defaultQuota: Int,
    val quotaConfigPath: String,
    val allowMissingConfig: Boolean,
    val refreshPeriodMinutes: Long
)
