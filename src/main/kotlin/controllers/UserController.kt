package controllers

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import services.UserService

fun Routing.apiUser(service: UserService) {
    route("/user") {
        post {
            val create = call.receive<CreateUser>()
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
                val change = call.receive<FullNameChange>()
                service.changeUser(userId, change.newFullName)
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
                    val create = call.receive<CreateAccount>()
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


data class FullNameChange(
    val newFullName: String
)

data class CreateAccount(val currencyName: String)

data class CreateUser(val fullName: String)