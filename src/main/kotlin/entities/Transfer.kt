package entities

import IDType
import MoneyType
import kotlinx.serialization.Serializable

@Serializable
final class Transfer(
    val id: IDType,
    val fromAccountId: IDType,
    val toAccountId: IDType,
    val amount: MoneyType,
    val currencyName: String,
    val timestamp: Long,
    val status: TransferStatus
) {
    fun updateStatus(updatedStatus: TransferStatus): Transfer {
        return Transfer(
            id,
            fromAccountId,
            toAccountId,
            amount,
            currencyName,
            timestamp,
            updatedStatus
        )
    }
}

enum class TransferStatus {
    PROCESSING, SUCCEEDED, FAILED
}