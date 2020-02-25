package services

import entities.*
import errors.*
import kotlinx.atomicfu.atomic
import kotlinx.collections.immutable.persistentMapOf
import types.IDType
import types.MoneyType
import types.ZeroMoney

class TransferService(private val accountService: AccountService, private val currencyService: CurrencyService) {
    private val transfersRef = atomic(persistentMapOf<IDType, Transfer>())
    private val idGenerator = IdGenerator()

    /**
     * Returns a snapshot of the given transfer.
     *
     * Transfers after creation are never modified (its fields are final), so no mutex is needed.
     *
     * @throws IdNotFoundException if transfer with the specified [id] doesn't exist.
     */
    fun getTransfer(id: IDType): TransferDTO {
        val transfer = transfersRef.value[id] ?: throwTransferNotFound(id)
        return transfer.toDTO()
    }

    /**
     * Creates and returns a transfer representing money transaction from account [fromAccountId] to account [toAccountId].
     *
     * Acquires locks of the from and to accounts in specific order.
     * (see [AccountService.acquireAccountLock])
     * Acquires locks of the from account's currency, to account's currency and the given transaction currency in specific order.
     * (see [CurrencyService.acquireCurrencyLock])
     * Creates the transfer.
     * Adds the created transfer to the storage using CAS (multiple tries could be needed).
     * Release locks of the involved currencies.
     * Release locks of the involved accounts.
     *
     * @throws CreateNotAllowedException if [fromAccountId] is equal to [toAccountId].
     * @throws IdNotFoundException if there is no account with the specified [fromAccountId] or [toAccountId].
     */
    fun createTransfer(fromAccountId: IDType, toAccountId: IDType, amount: MoneyType, currencyName: String): TransferDTO {
        require(amount >= ZeroMoney)

        if (fromAccountId == toAccountId)
            throw CreateNotAllowedException("Transfer from an account(id = $fromAccountId) to the same account(id = $toAccountId) is not allowed")

        val accounts = accountService.acquireAccountLock(fromAccountId, toAccountId)

        return accounts.releaseLockAfter {
            val fromAccount = accounts.first { it.id == fromAccountId }
            val toAccount = accounts.first { it.id == toAccountId }

            val currencies = currencyService.acquireCurrencyLock(
                fromAccount.currencyName, toAccount.currencyName, currencyName
            )

            currencies.releaseLockAfter {
                val fromAccountCurrency = currencies.first { it.name == fromAccount.currencyName }
                val toAccountCurrency = currencies.first { it.name == toAccount.currencyName }
                val transferCurrency = currencies.first { it.name == currencyName }

                val withdrawAmount = transferCurrency.convert(amount, fromAccountCurrency)
                val depositAmount = transferCurrency.convert(amount, toAccountCurrency)

                val status = if (fromAccount.balance < withdrawAmount) {
                    TransferStatus.FAILED
                } else {
                    fromAccount.updateBalance(-withdrawAmount)
                    toAccount.updateBalance(depositAmount)
                    TransferStatus.SUCCEEDED
                }

                val timestamp = System.currentTimeMillis()
                var transfer: Transfer
                do {
                    val transfers = transfersRef.value
                    val id = idGenerator.nextSuitable { !transfers.containsKey(it) }
                    transfer = Transfer(id, fromAccountId, toAccountId, amount, currencyName, timestamp, status)
                } while (!transfersRef.compareAndSet(transfers, transfers.put(id, transfer)))

                transfer.toDTO()
            }
        }
    }

    /**
     * Returns at most [limit] transfers where the given account is involved, after skipping first [offset] account's transfers.
     *
     * A persistent linked hash map is used as the storage, therefor, the entries are already sorted by there timestamp.
     */
    fun getTransfers(accountId: IDType, offset: Int, limit: Int): Set<TransferDTO> {
        return transfersRef.value.values.asSequence()
            .filter { it.fromAccountId == accountId || it.toAccountId == accountId }
            .drop(offset)
            .take(limit)
            .map { it.toDTO() }
            .toSet()
    }
}