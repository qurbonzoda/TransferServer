package services

import CreateNotAllowedException
import IDType
import IdNotFoundException
import MoneyType
import entities.Transfer
import entities.TransferStatus
import java.util.concurrent.ConcurrentHashMap

final class TransferService(private val accountService: AccountService, private val currencyService: CurrencyService) {
    private val transfers = ConcurrentHashMap<IDType, Transfer>()
    private val idGenerator = IdGenerator()

    fun getTransfer(id: IDType): Transfer {
        return transfers[id]
            ?: throw IdNotFoundException("Transfer with the given id: $id doesn't exist")
    }

    fun createTransfer(fromAccountId: IDType, toAccountId: IDType, amount: MoneyType, currencyName: String): Transfer {
        if (fromAccountId == toAccountId)
            throw CreateNotAllowedException("Transfer from an account(id = $fromAccountId) to the same account(id = $toAccountId) is not allowed")

        var transfer: Transfer?
        do {
            val id = idGenerator.next()
            val timestamp = System.currentTimeMillis()
            transfer = Transfer(
                id,
                fromAccountId,
                toAccountId,
                amount,
                currencyName,
                timestamp,
                TransferStatus.PROCESSING
            )
        } while (transfers.putIfAbsent(id, transfer!!) != null)


        // TODO: Convert the given amount to the right currency
        currencyService.acquireLock()

        var nonExistentAccountId: Int? = null

        if (fromAccountId < toAccountId) {
            val newFromAccount = accountService.acquireLock(fromAccountId) { _, fromAccount ->
                val newToAccount = accountService.acquireLock(toAccountId) { _, toAccount -> toAccount.diff(amount) }

                if (newToAccount == null) {
                    nonExistentAccountId = toAccountId
                    fromAccount
                } else {
                    fromAccount.diff(-amount)
                }
            }

            if (newFromAccount == null) {
                nonExistentAccountId = fromAccountId
            }
        } else {
            val newToAccount = accountService.acquireLock(toAccountId) { _, toAccount ->
                val newFromAccount = accountService.acquireLock(fromAccountId) { _, fromAccount -> fromAccount.diff(-amount) }

                if (newFromAccount == null) {
                    nonExistentAccountId = fromAccountId
                    toAccount
                } else {
                    toAccount.diff(amount)
                }
            }

            if (newToAccount == null) {
                nonExistentAccountId = toAccountId
            }
        }

        currencyService.releaseLock()

        // TODO: Maybe throw exception if any of the given accounts does not exist
        val processedTransfer = transfer.updateStatus(
            if (nonExistentAccountId != null) TransferStatus.FAILED else TransferStatus.SUCCEEDED
        )
        transfers[transfer.id] = processedTransfer

        return processedTransfer
    }

    fun getTransfers(accountId: IDType, offset: Int, limit: Int): Set<Transfer> {
        return transfers.values.asSequence()
            .filter { it.fromAccountId == accountId || it.toAccountId == accountId }
            .drop(offset)
            .take(limit)
            .toSet()
    }
}