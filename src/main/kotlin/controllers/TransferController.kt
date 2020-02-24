package controllers

import BadRequest
import IDType
import MoneyType
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
            val dto = call.receive<TransferDTO>()
            if (dto.amount < 0) throw BadRequest("Transfer amount: ${dto.amount} should be greater than zero")
            val transfer = service.createTransfer(dto.fromAccountId, dto.toAccountId, dto.amount, dto.currencyName)
            call.respond(transfer)
        }
    }
}

@Serializable
data class TransferDTO(
    val fromAccountId: IDType,
    val toAccountId: IDType,
    val amount: MoneyType,
    val currencyName: String
)