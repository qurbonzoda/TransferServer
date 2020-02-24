package controllers

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import kotlinx.serialization.Serializable
import services.UserService

fun Routing.apiUser(service: UserService) {
    route("/user") {
        post {
            val create = call.receive<CreateUserRequest>()
            val user = service.createUser(create.fullName)
            call.respond(user.id)
        }

        route("{userId}") {
            get {
                val userId = call.validateId("userId")
                val user = service.getUser(userId)
                call.respond(user)
            }

            put {
                val userId = call.validateId("userId")
                val update = call.receive<UpdateUserRequest>()
                service.updateUser(userId, update.newFullName)
                call.respond(HttpStatusCode.OK)
            }

            delete {
                val userId = call.validateId("userId")
                service.deleteUser(userId)
                call.respond(HttpStatusCode.OK)
            }

            route("account") {
                post {
                    val userId = call.validateId("userId")
                    val create = call.receive<CreateAccountRequest>()
                    val account = service.createAccount(userId, create.currencyName)
                    call.respond(account.id)
                }

                delete("{accountId}") {
                    val userId = call.validateId("userId")
                    val accountId = call.validateId("accountId")
                    service.deleteAccount(userId, accountId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

@Serializable
data class UpdateUserRequest(
    val newFullName: String
)

@Serializable
data class CreateAccountRequest(val currencyName: String)

@Serializable
data class CreateUserRequest(val fullName: String)