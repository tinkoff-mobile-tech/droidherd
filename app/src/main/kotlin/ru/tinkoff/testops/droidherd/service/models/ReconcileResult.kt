package ru.tinkoff.testops.droidherd.service.models

import io.kubernetes.client.extended.controller.reconciler.Result

class ReconcileResult(
    val result: Result,
    val status: Status,
    val details: String = ""
) {

    enum class Status {
        Creating,
        Reducing,
        Pending,
        Reconciled,
        Deleted,
        QuotaPatched,
        StatusUpdated,
        Error
    }
}
