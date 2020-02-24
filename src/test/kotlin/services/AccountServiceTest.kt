package services

import BadRequest
import DeleteNotAllowedException
import IDType
import IdNotFoundException
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AccountServiceTest {
    private fun createAccountService(): AccountService {
        val currencyService = CurrencyService()
        return AccountService(currencyService)
    }

    @Test
    fun createAccount() {
        val accountService = createAccountService()

        val ids = mutableSetOf<IDType>()

        repeat(100) {
            val account = accountService.createAccount("RUB")
            assertEquals(0.0, account.balance)
            assertEquals("RUB", account.currencyName)

            assertTrue(ids.add(account.id))
        }
    }

    @Test
    fun getAccount() {
        val accountService = createAccountService()

        assertFailsWith<BadRequest> { accountService.getAccount(0) }

        val ids = mutableSetOf<IDType>()

        repeat(100) {
            val account = accountService.createAccount("RUB")
            ids.add(account.id)
        }

        repeat(100) { index ->
            val accountId = ids.elementAt(index)
            val account = accountService.getAccount(accountId)
            assertEquals(accountId, account.id)
            assertEquals(0.0, account.balance)
            assertEquals("RUB", account.currencyName)
        }
    }

    @Test
    fun depositIntoAccount() {
        val accountService = createAccountService()

        assertFailsWith<IdNotFoundException> { accountService.depositIntoAccount(0, 10.0, "RUB") }
        assertFailsWith<IllegalArgumentException> { accountService.depositIntoAccount(0, -10.0, "RUB") }

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

            val account = accountService.getAccount(accountId)
            assertEquals(accountId, account.id)
            assertEquals(20.0, account.balance)
            assertEquals("RUB", account.currencyName)
        }
    }

    @Test
    fun withdrawFromAccount() {
        val accountService = createAccountService()

        assertFailsWith<IdNotFoundException> { accountService.withdrawFromAccount(0, 10.0, "RUB") }
        assertFailsWith<IllegalArgumentException> { accountService.withdrawFromAccount(0, -10.0, "RUB") }

        val ids = mutableSetOf<IDType>()

        repeat(100) {
            val accountId = accountService.createAccount("RUB").id
            ids.add(accountId)
        }

        repeat(100) { index ->
            val accountId = ids.elementAt(index)
            accountService.withdrawFromAccount(accountId, 10.0, "RUB")
            assertFailsWith<IllegalArgumentException> { accountService.withdrawFromAccount(accountId, -10.0, "RUB") }
            accountService.withdrawFromAccount(accountId, 10.0, "USD")

            val account = accountService.getAccount(accountId)
            assertEquals(accountId, account.id)
            assertEquals(-20.0, account.balance)
            assertEquals("RUB", account.currencyName)
        }
    }

    @Test
    fun deleteAccount() {
        val accountService = createAccountService()

        assertFailsWith<BadRequest> { accountService.deleteAccount(0) }

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