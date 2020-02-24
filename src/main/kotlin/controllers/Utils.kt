package controllers

import BadRequest
import IDType
import io.ktor.application.ApplicationCall

fun ApplicationCall.validateId(name: String): IDType {
    val stringId = this.parameters[name]!!
    return stringId.toIntOrNull() ?: throw BadRequest("Invalid $name: $stringId")
}

fun ApplicationCall.validateQueryParameter(name: String, default: Int): Int {
    require(default >= 0)
    val stringValue = this.request.queryParameters[name] ?: return default
    val intValue = stringValue.toIntOrNull()
    if (intValue == null || intValue < 0) throw BadRequest("Invalid $name: $stringValue")

    return intValue
}