import java.util.*

final class Transfer(
    val id: IDType,
    val fromAccount: Account,
    val toAccount: Account,
    val amount: MoneyType,
    val currency: Currency,
    val date: Date,
    val status: TransferStatus
)

enum class TransferStatus {
    SUBMITTED, PROCESSING, SUCCEEDED, FAILED
}