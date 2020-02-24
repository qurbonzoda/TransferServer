package services

import BadRequest
import IDType
import IdNotFoundException
import entities.Transfer
import entities.TransferStatus
import org.junit.Test
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

final class TransferServiceTest {
    @Test
    fun getTransfer() {
        val currencyService = CurrencyService()
        val accountService = AccountService(currencyService)
        val transferService = TransferService(accountService, currencyService)

        assertFailsWith<IdNotFoundException> { transferService.getTransfer(0) }

        val accountId1 = accountService.createAccount("RUB").id
        val accountId2 = accountService.createAccount("RUB").id
        val accountId3 = accountService.createAccount("USD").id
        val accountId4 = accountService.createAccount("EUR").id

        val accountIds = setOf(accountId1, accountId2, accountId3, accountId4)

        val ids = mutableSetOf<IDType>()

        repeat(100) { index ->
            val fromAccountId = accountIds.elementAt(index % accountIds.size)
            val toAccountId = accountIds.elementAt((index + 1) % accountIds.size)
            val amount = index.toDouble()

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
            assertEquals(TransferStatus.SUCCEEDED, transfer.status)
        }
    }

    @Test
    fun createTransfer() {
        val currencyService = CurrencyService()
        val accountService = AccountService(currencyService)
        val transferService = TransferService(accountService, currencyService)

        assertEquals(TransferStatus.FAILED, transferService.createTransfer(0, 1, 10.0, "RUB").status)

        val accountId1 = accountService.createAccount("RUB").id
        val accountId2 = accountService.createAccount("RUB").id
        val accountId3 = accountService.createAccount("USD").id
        val accountId4 = accountService.createAccount("EUR").id

        val accountIds = setOf(accountId1, accountId2, accountId3, accountId4)

        val ids = mutableSetOf<IDType>()

        repeat(100) {
            val fromAccountId = accountIds.random()
            val toAccountId = (accountIds - fromAccountId).random()
            val amount = Random.nextInt(256).toDouble()

            assertFailsWith<BadRequest> { transferService.createTransfer(fromAccountId, fromAccountId, amount, "RUB") }

            val fromAccountBalance = accountService.getAccount(fromAccountId).balance
            val toAccountBalance = accountService.getAccount(toAccountId).balance

            val transfer = transferService.createTransfer(fromAccountId, toAccountId, amount, "RUB")
            assertEquals(fromAccountId, transfer.fromAccountId)
            assertEquals(toAccountId, transfer.toAccountId)
            assertEquals(amount, transfer.amount)
            assertEquals("RUB", transfer.currencyName)
            assertTrue(System.currentTimeMillis() >= transfer.timestamp)
            assertEquals(TransferStatus.SUCCEEDED, transfer.status)

            val  eps = 1E-6
            accountService.getAccount(fromAccountId).balance.let { actual ->
                val expected = (fromAccountBalance - amount)
                assertTrue((expected - actual).absoluteValue < eps, "amount: $amount, expected: $expected, actual: $actual")
            }
            accountService.getAccount(toAccountId).balance.let { actual ->
                val expected = (toAccountBalance + amount)
                assertTrue((expected - actual).absoluteValue < eps, "amount: $amount, expected: $expected, actual: $actual")
            }

            assertTrue(ids.add(transfer.id))
        }
    }

    @Test
    fun getTransfers() {
        val currencyService = CurrencyService()
        val accountService = AccountService(currencyService)
        val transferService = TransferService(accountService, currencyService)

        val accountId1 = accountService.createAccount("RUB").id
        val accountId2 = accountService.createAccount("RUB").id
        val accountId3 = accountService.createAccount("USD").id
        val accountId4 = accountService.createAccount("EUR").id

        val accountIds = setOf(accountId1, accountId2, accountId3, accountId4)

        accountIds.forEach {
            assertTrue(transferService.getTransfers(it, 0, 10).isEmpty())
        }

        val accountIdToTransferIds = mutableMapOf<IDType, MutableSet<IDType>>()

        repeat(100) {
            val fromAccountId = accountIds.random()
            val toAccountId = (accountIds - fromAccountId).random()
            val amount = Random.nextInt(256).toDouble()

            val transferId = transferService.createTransfer(fromAccountId, toAccountId, amount, "RUB").id

            val fromAccountTransfers = accountIdToTransferIds.getOrPut(fromAccountId, { mutableSetOf() })
            fromAccountTransfers.add(transferId)

            val toAccountTransfers = accountIdToTransferIds.getOrPut(toAccountId, { mutableSetOf() })
            toAccountTransfers.add(transferId)
        }

        accountIds.forEach {
            val expected = accountIdToTransferIds[it]!!
            val offset = Random.nextInt(0..expected.size)
            val limit = Random.nextInt(0..expected.size)

            val actual = transferService.getTransfers(it, offset, limit).map(Transfer::id).toSet()
            assertEquals(expected.drop(offset).take(limit).toSet(), actual)
        }
    }
}