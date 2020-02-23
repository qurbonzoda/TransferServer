package controllers

import BadRequest
import IDType
import io.ktor.application.ApplicationCall

fun ApplicationCall.validateId(name: String): IDType {
    val stringId = this.parameters[name]!!
    return stringId.toIntOrNull() ?: throw BadRequest("Invalid $name: $stringId")
}