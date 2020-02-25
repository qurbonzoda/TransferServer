package services

import errors.BadRequest
import types.IDType
import errors.IdNotFoundException
import entities.TransferDTO
import entities.TransferStatus
import org.junit.Test
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

final class TransferServiceTest {

    private fun initialize(currencyService: CurrencyService, accountService: AccountService): Set<IDType> {
        currencyService.createCurrency("RUB", 64.07)
        currencyService.createCurrency("USD", 1.0)
        currencyService.createCurrency("EUR", 0.92)
        val accountId1 = accountService.createAccount("RUB").id
        val accountId2 = accountService.createAccount("RUB").id
        val accountId3 = accountService.createAccount("USD").id
        val accountId4 = accountService.createAccount("EUR").id

        val accountIds = setOf(accountId1, accountId2, accountId3, accountId4)

        accountIds.random().let { accountService.depositIntoAccount(it, 5000.0, "RUB") }

        return accountIds
    }

    @Test
    fun getTransfer() {
        val currencyService = CurrencyService()
        val accountService = AccountService(currencyService)
        val transferService = TransferService(accountService, currencyService)

        assertFailsWith<IdNotFoundException> { transferService.getTransfer(0) }

        val accountIds = initialize(currencyService, accountService)

        val ids = mutableSetOf<IDType>()
        val statuses = mutableListOf<TransferStatus>()

        repeat(100) { index ->
            val fromAccountId = accountIds.elementAt(index % accountIds.size)
            val toAccountId = accountIds.elementAt((index + 1) % accountIds.size)
            val amount = index.toDouble()

            val rub = currencyService.getCurrency("RUB")
            val fromAccount = accountService.getAccount(fromAccountId)
            val fromCurrency = currencyService.getCurrency(fromAccount.currencyName)

            if (rub.convert(amount, fromCurrency) <= fromAccount.balance) {
                statuses.add(TransferStatus.SUCCEEDED)
            } else {
                statuses.add(TransferStatus.FAILED)
            }

            val transfer = transferService.createTransfer(fromAccountId, toAccountId, amount, "RUB")
            ids.add(transfer.id)
        }

        repeat(100) { index ->
            val transferId = ids.elementAt(index)
            val fromAccountId = accountIds.elementAt(index % accountIds.size)
            val toAccountId = accountIds.elementAt((index + 1) % accountIds.size)
            val amount = index.toDouble()

            val transfer = transferService.getTransfer(transferId)
            assertEquals(transferId, transfer.id)
            assertEquals(fromAccountId, transfer.fromAccountId)
            assertEquals(toAccountId, transfer.toAccountId)
            assertEquals(amount, transfer.amount)
            assertEquals("RUB", transfer.currencyName)
            assertTrue(System.currentTimeMillis() >= transfer.timestamp)
            assertEquals(statuses[index], transfer.status)
        }
    }

    @Test
    fun createTransfer() {
        val currencyService = CurrencyService()
        val accountService = AccountService(currencyService)
        val transferService = TransferService(accountService, currencyService)

        assertFailsWith<IdNotFoundException> {
            transferService.createTransfer(0, 1, 10.0, "RUB")
        }

        val accountIds = initialize(currencyService, accountService)

        val ids = mutableSetOf<IDType>()

        repeat(100) {
            val fromAccountId = accountIds.random()
            val toAccountId = (accountIds - fromAccountId).random()
            val amount = Random.nextInt(256).toDouble()

            assertFailsWith<BadRequest> { transferService.createTransfer(fromAccountId, fromAccountId, amount, "RUB") }

            val rub = currencyService.getCurrency("RUB")
            val fromAccount = accountService.getAccount(fromAccountId)
            val fromCurrency = currencyService.getCurrency(fromAccount.currencyName)
            val toAccount = accountService.getAccount(toAccountId)
            val toCurrency = currencyService.getCurrency(toAccount.currencyName)

            val status = if (rub.convert(amount, fromCurrency) <= fromAccount.balance) {
                TransferStatus.SUCCEEDED
            } else {
                TransferStatus.FAILED
            }

            val transfer = transferService.createTransfer(fromAccountId, toAccountId, amount, "RUB")
            assertEquals(fromAccountId, transfer.fromAccountId)
            assertEquals(toAccountId, transfer.toAccountId)
            assertEquals(amount, transfer.amount)
            assertEquals("RUB", transfer.currencyName)
            assertTrue(System.currentTimeMillis() >= transfer.timestamp)
            assertEquals(status, transfer.status)

            val  eps = 1E-6
            val fromActual = accountService.getAccount(fromAccountId).balance
            val toActual = accountService.getAccount(toAccountId).balance

            if (status == TransferStatus.FAILED) {
                assertEquals(fromAccount.balance, fromActual)
                assertEquals(toAccount.balance, toActual)
            } else {
                val fromExpected = fromAccount.balance - rub.convert(amount, fromCurrency)
                val toExpected = toAccount.balance + rub.convert(amount, toCurrency)
                assertTrue((fromExpected - fromActual).absoluteValue < eps)
                assertTrue((toExpected - toActual).absoluteValue < eps)
            }

            assertTrue(ids.add(transfer.id))
        }
    }

    @Test
    fun getTransfers() {
        val currencyService = CurrencyService()
        val accountService = AccountService(currencyService)
        val transferService = TransferService(accountService, currencyService)

        val accountIds = initialize(currencyService, accountService)

        accountIds.forEach {
            assertTrue(transferService.getTransfers(it, 0, 10).isEmpty())
        }

        val accountIdToTransferIds = mutableMapOf<IDType, MutableSet<IDType>>()

        repeat(100) {
            val fromAccountId = accountIds.random()
            val toAccountId = (accountIds - fromAccountId).random()
            val amount = Random.nextInt(256).toDouble()

            val transfer = transferService.createTransfer(fromAccountId, toAccountId, amount, "RUB")

            val fromAccountTransfers = accountIdToTransferIds.getOrPut(fromAccountId, { mutableSetOf() })
            fromAccountTransfers.add(transfer.id)

            val toAccountTransfers = accountIdToTransferIds.getOrPut(toAccountId, { mutableSetOf() })
            toAccountTransfers.add(transfer.id)
        }

        accountIds.forEach { accountId ->
            val expected = accountIdToTransferIds[accountId]!!
            val offset = Random.nextInt(0..expected.size)
            val limit = Random.nextInt(0..expected.size)

            val actual = transferService.getTransfers(accountId, offset, limit).map(TransferDTO::id)
            assertEquals(expected.drop(offset).take(limit), actual)
        }
    }
}