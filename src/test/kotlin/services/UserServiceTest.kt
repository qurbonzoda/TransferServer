package services

import BadRequest
import DeleteNotAllowedException
import IDType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UserServiceTest {
    private fun createAccountService(): AccountService {
        val currencyService = CurrencyService()
        return AccountService(currencyService)
    }

    private fun createUserService(): UserService {
        val accountService = createAccountService()
        return UserService(accountService)
    }

    @Test
    fun createUser() {
        val userService = createUserService()

        val ids = mutableSetOf<IDType>()

        repeat(100) { index ->
            val user = userService.createUser("Abduqodiri Qurbonzoda-$index")
            assertEquals("Abduqodiri Qurbonzoda-$index", user.fullName)
            assertTrue(user.accountIds.isEmpty())

            assertTrue(ids.add(user.id))
        }
    }

    @Test
    fun getUser() {
        val userService = createUserService()

        assertFailsWith<BadRequest> { userService.getUser(0) }

        val ids = mutableSetOf<IDType>()

        repeat(100) { index ->
            val userId = userService.createUser("Abduqodiri Qurbonzoda-$index").id
            ids.add(userId)
        }

        repeat(100) { index ->
            val userId = ids.elementAt(index)
            val user = userService.getUser(userId)
            assertEquals(userId, user.id)
            assertEquals("Abduqodiri Qurbonzoda-$index", user.fullName)
            assertTrue(user.accountIds.isEmpty())
        }
    }

    @Test
    fun changeUser() {
        val userService = createUserService()

        assertFailsWith<BadRequest> { userService.changeUser(0, "Abduqodiri Qurbonzoda") }

        val ids = mutableSetOf<IDType>()

        repeat(100) { index ->
            val user = userService.createUser("Abduqodiri Qurbonzoda-$index")
            assertEquals("Abduqodiri Qurbonzoda-$index", user.fullName)
            assertTrue(user.accountIds.isEmpty())

            ids.add(user.id)
        }

        repeat(100) { index ->
            val userId = ids.elementAt(index)
            userService.changeUser(userId, "Nikita Nozhkin-$index")
            assertEquals("Nikita Nozhkin-$index", userService.getUser(userId).fullName)
        }
    }

    @Test
    fun deleteUser() {
        val userService = createUserService()

        assertFailsWith<BadRequest> { userService.deleteUser(0) }

        val ids = mutableSetOf<IDType>()

        repeat(100) { index ->
            val userId = userService.createUser("Abduqodiri Qurbonzoda-$index").id
            ids.add(userId)
        }

        repeat(100) { index ->
            val userId = ids.elementAt(index)

            val accountId = userService.createAccount(userId, "RUB").id
            assertFailsWith<DeleteNotAllowedException> { userService.deleteUser(userId) }

            userService.deleteAccount(userId, accountId)
            userService.deleteUser(userId)
            
            assertFailsWith<BadRequest> { userService.getUser(userId) }
            assertFailsWith<BadRequest> { userService.changeUser(userId, "Nikita Nozhkin-$index") }
            assertFailsWith<BadRequest> { userService.deleteUser(userId) }
        }
    }

    @Test
    fun createAccount() {
        val accountService = createAccountService()
        val userService = UserService(accountService)

        assertFailsWith<BadRequest> { userService.createAccount(0, "RUB") }

        val userId = userService.createUser("Abduqodiri Qurbonzoda").id

        val ids = mutableSetOf<IDType>()

        repeat(100) {
            val account = userService.createAccount(userId, "RUB")
            assertEquals("RUB", account.currencyName)
            assertEquals(0.0, account.balance)

            assertTrue(ids.add(account.id))

            assertEquals(ids, userService.getUser(userId).accountIds)

            assertEquals(account, accountService.getAccount(account.id))
        }
    }

    @Test
    fun deleteAccount() {
        val accountService = createAccountService()
        val userService = UserService(accountService)

        assertFailsWith<BadRequest> { userService.deleteAccount(0, 0) }

        val userId = userService.createUser("Abduqodiri Qurbonzoda").id

        assertFailsWith<BadRequest> { userService.deleteAccount(userId, 0) }

        val ids = mutableSetOf<IDType>()

        repeat(100) {
            val accountId = userService.createAccount(userId, "RUB").id
            ids.add(accountId)
        }

        repeat(100) { index ->
            val accountId = ids.elementAt(index)

            accountService.depositIntoAccount(accountId, 10.0, "RUB")
            assertFailsWith<DeleteNotAllowedException> { userService.deleteAccount(userId, accountId) }

            accountService.withdrawFromAccount(accountId, 10.0, "RUB")
            userService.deleteAccount(userId, accountId)

            assertFailsWith<BadRequest> { accountService.getAccount(accountId) }
            assertFailsWith<BadRequest> { accountService.deleteAccount(accountId) }

            assertEquals(ids.drop(index + 1).toSet(), userService.getUser(userId).accountIds)
        }
    }
}