package ru.tinkoff.testops.droidherd.service.kubernetes

import ru.tinkoff.testops.droidherd.service.configs.DroidherdConfig
import ru.tinkoff.testops.droidherd.service.models.DroidherdResource

class TemplateParameters(
    generatedName: String, resource: DroidherdResource, image: String, config: DroidherdConfig
) {
    val asMap = mapOf(
        "Name" to generatedName,
        "DroidherdSessionName" to resource.getName(),
        "Image" to image,
        "FullQualifiedImage" to (config.allowedImages[image] ?: throw IllegalArgumentException("Image $image not allowed")),
        "ClientId" to resource.getSession().clientId,
        "SessionId" to resource.getSession().sessionId,
        "EmulatorArgs" to config.emulatorArgs,
        "EmulatorProxy" to config.emulatorProxy,
        "Port" to config.servicePort,
        "UID" to resource.getUid(),
        "EmulatorContainerName" to config.emulatorContainerName
    )
}
