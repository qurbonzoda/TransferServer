package controllers

import errors.BadRequest
import types.IDType
import types.MoneyType
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.put
import io.ktor.routing.route
import kotlinx.serialization.Serializable
import services.AccountService

fun Routing.apiAccount(service: AccountService) {
    route("/account") {
        get("{accountId}") {
            val accountId = call.validateId("accountId")
            val account = service.getAccount(accountId)
            call.respond(account)
        }

        // e.g. from ATM
        put("deposit") {
            val deposit = call.receive<DepositRequest>()
            if (deposit.amount < 0) throw BadRequest("Deposit amount: ${deposit.amount} is negative")
            service.depositIntoAccount(deposit.id, deposit.amount, deposit.currencyName)
            call.respond(HttpStatusCode.OK)
        }

        put("withdraw") {
            val withdraw = call.receive<WithdrawRequest>()
            if (withdraw.amount < 0) throw BadRequest("Withdraw amount: ${withdraw.amount} is negative")
            service.withdrawFromAccount(withdraw.id, withdraw.amount, withdraw.currencyName)
            call.respond(HttpStatusCode.OK)
        }
    }
}

@Serializable
data class DepositRequest(
    val id: IDType,
    val amount: MoneyType,
    val currencyName: String
)

@Serializable
data class WithdrawRequest(
    val id: IDType,
    val amount: MoneyType,
    val currencyName: String
)