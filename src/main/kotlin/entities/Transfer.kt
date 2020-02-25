package entities

import types.IDType
import types.MoneyType
import kotlinx.serialization.Serializable

class Transfer(
    val id: IDType,
    val fromAccountId: IDType,
    val toAccountId: IDType,
    val amount: MoneyType,
    val currencyName: String,
    val timestamp: Long,
    val status: TransferStatus
) {
    fun toDTO(): TransferDTO {
        return TransferDTO(id, fromAccountId, toAccountId, amount, currencyName, timestamp, status)
    }
}


@Serializable
data class TransferDTO(
    val id: IDType,
    val fromAccountId: IDType,
    val toAccountId: IDType,
    val amount: MoneyType,
    val currencyName: String,
    val timestamp: Long,
    val status: TransferStatus
)

@Serializable
enum class TransferStatus {
    SUCCEEDED, FAILED
}

