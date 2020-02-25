package services

import errors.BadRequest
import errors.DeleteNotAllowedException
import errors.IdNotFoundException
import types.IDType
import org.junit.Test
import types.ZeroMoney
import kotlin.math.absoluteValue
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AccountServiceTest {
    @Test
    fun createAccount() {
        val currencyService = CurrencyService()
        val accountService = AccountService(currencyService)

        currencyService.createCurrency("RUB", 64.07)

        val ids = mutableSetOf<IDType>()

        repeat(100) {
            val account = accountService.createAccount("RUB")
            assertEquals(ZeroMoney, account.balance)
            assertEquals("RUB", account.currencyName)

            assertTrue(ids.add(account.id))
        }
    }

    @Test
    fun getAccount() {
        val currencyService = CurrencyService()
        val accountService = AccountService(currencyService)

        assertFailsWith<BadRequest> { accountService.getAccount(0) }

        currencyService.createCurrency("RUB", 64.07)

        val ids = mutableSetOf<IDType>()

        repeat(100) {
            val account = accountService.createAccount("RUB")
            ids.add(account.id)
        }

        repeat(100) { index ->
            val accountId = ids.elementAt(index)
            val account = accountService.getAccount(accountId)
            assertEquals(accountId, account.id)
            assertEquals(ZeroMoney, account.balance)
            assertEquals("RUB", account.currencyName)
        }
    }

    @Test
    fun depositIntoAccount() {
        val currencyService = CurrencyService()
        val accountService = AccountService(currencyService)


        assertFailsWith<IdNotFoundException> { accountService.depositIntoAccount(0, 10.0, "RUB") }
        assertFailsWith<IllegalArgumentException> { accountService.depositIntoAccount(0, -10.0, "RUB") }

        currencyService.createCurrency("RUB", 64.07)
        currencyService.createCurrency("USD", 1.0)

        val ids = mutableSetOf<IDType>()

        repeat(100) {
            val account = accountService.createAccount("RUB")
            ids.add(account.id)
        }

        repeat(100) { index ->
            val accountId = ids.elementAt(index)
            accountService.depositIntoAccount(accountId, 10.0, "RUB")
            assertFailsWith<IllegalArgumentException> { accountService.depositIntoAccount(accountId, -10.0, "RUB") }
            accountService.depositIntoAccount(accountId, 10.0, "USD")

            val eps = 1E-6
            val account = accountService.getAccount(accountId)
            assertEquals(accountId, account.id)
            assertTrue((650.7 - account.balance).absoluteValue < eps)
            assertEquals("RUB", account.currencyName)
        }
    }

    @Test
    fun withdrawFromAccount() {
        val currencyService = CurrencyService()
        val accountService = AccountService(currencyService)

        assertFailsWith<IdNotFoundException> { accountService.withdrawFromAccount(0, 10.0, "RUB") }
        assertFailsWith<IllegalArgumentException> { accountService.withdrawFromAccount(0, -10.0, "RUB") }

        currencyService.createCurrency("RUB", 64.07)
        currencyService.createCurrency("USD", 1.0)

        val ids = mutableSetOf<IDType>()

        repeat(100) {
            val accountId = accountService.createAccount("RUB").id
            ids.add(accountId)
            accountService.depositIntoAccount(accountId, 5000.0, "RUB")
        }

        repeat(100) { index ->
            val accountId = ids.elementAt(index)
            accountService.withdrawFromAccount(accountId, 10.0, "RUB")
            assertFailsWith<IllegalArgumentException> { accountService.withdrawFromAccount(accountId, -10.0, "RUB") }
            accountService.withdrawFromAccount(accountId, 10.0, "USD")

            val eps = 1E-6
            val account = accountService.getAccount(accountId)
            assertEquals(accountId, account.id)
            assertTrue((4990.0 - 640.7 - account.balance).absoluteValue < eps)
            assertEquals("RUB", account.currencyName)
        }
    }

    @Test
    fun deleteAccount() {
        val currencyService = CurrencyService()
        val accountService = AccountService(currencyService)

        assertFailsWith<BadRequest> { accountService.deleteAccount(0) }

        currencyService.createCurrency("RUB", 64.07)

        val ids = mutableSetOf<IDType>()

        repeat(100) {
            val accountId = accountService.createAccount("RUB").id
            ids.add(accountId)
        }

        repeat(100) { index ->
            val accountId = ids.elementAt(index)

            accountService.depositIntoAccount(accountId, 10.0, "RUB")
            assertFailsWith<DeleteNotAllowedException> { accountService.deleteAccount(accountId) }

            accountService.withdrawFromAccount(accountId, 10.0, "RUB")
            accountService.deleteAccount(accountId)

            assertFailsWith<BadRequest> { accountService.getAccount(accountId) }
            assertFailsWith<BadRequest> { accountService.depositIntoAccount(accountId, 10.0, "RUB") }
            assertFailsWith<BadRequest> { accountService.withdrawFromAccount(accountId, 10.0, "RUB") }
            assertFailsWith<BadRequest> { accountService.deleteAccount(accountId) }
        }
    }
}