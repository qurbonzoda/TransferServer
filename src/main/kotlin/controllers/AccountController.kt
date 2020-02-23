package controllers

import IDType
import MoneyType
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.put
import io.ktor.routing.route
import services.AccountService

fun Routing.apiAccount(service: AccountService) {
    route("/account") {
        get("{accountId}") {
            val accountId = call.validateId("accountId")
            val account = service.getAccount(accountId)
            call.respond(account)
        }

        // e.g. from ATM
        put {
            val diff = call.receive<BalanceDiff>()
            service.changeAccount(diff.id, diff.diff, diff.currencyName)
            call.respond(HttpStatusCode.OK)
        }
    }
}

data class BalanceDiff(
    val id: IDType,
    val diff: MoneyType,
    val currencyName: String
)