package controllers

import errors.BadRequest
import types.IDType
import types.MoneyType
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import kotlinx.serialization.Serializable
import services.TransferService

fun Routing.apiTransfer(service: TransferService) {
    get("/transfers/account/{accountId}") {
        val accountId = call.validateId("accountId")
        val offset = call.validateQueryParameter("offset", 0)
        val limit = call.validateQueryParameter("limit", Int.MAX_VALUE)

        val transfers = service.getTransfers(accountId, offset, limit)
        call.respond(transfers)
    }

    route("/transfer") {
        get("{transferId}") {
            val transferId = call.validateId("transferId")
            val transfer = service.getTransfer(transferId)
            call.respond(transfer)
        }

        post {
            val create = call.receive<CreateTransferRequest>()
            if (create.amount < 0) throw BadRequest("Transfer amount: ${create.amount} is negative")
            val transfer = service.createTransfer(create.fromAccountId, create.toAccountId, create.amount, create.currencyName)
            call.respond(transfer)
        }
    }
}

@Serializable
data class CreateTransferRequest(
    val fromAccountId: IDType,
    val toAccountId: IDType,
    val amount: MoneyType,
    val currencyName: String
)