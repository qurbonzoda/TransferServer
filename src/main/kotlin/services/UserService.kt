package services

import Account
import DeleteNotAllowedException
import IDType
import IdNotFoundException
import User
import java.util.concurrent.ConcurrentHashMap

final class UserService(private val accountService: AccountService) {
    private val users = ConcurrentHashMap<IDType, User>()
    private val idGenerator = IdGenerator()

    fun getUser(id: IDType): User {
        return users[id]
            ?: throw IdNotFoundException("User with the given id: \"$id\" doesn't exist")
    }

    fun createUser(fullName: String): User {
        while (true) {
            val id = idGenerator.next()
            val user = User(id, fullName, emptyList())
            users.putIfAbsent(id, user) ?: return user
        }
    }

    fun changeUser(id: IDType, newFullName: String) {
        users.computeIfPresent(id) { _, oldUser -> oldUser.changeFullName(newFullName) }
            ?: throw IdNotFoundException("User with the given id: \"$id\" doesn't exist")
    }

    fun deleteUser(id: IDType) {
        while (true) {
            val user = getUser(id)
            val accountCount = user.accounts.size
            if (accountCount != 0)
                throw DeleteNotAllowedException("The given user has $accountCount account(s). Firstly remove the user's all accounts")

            if (users.remove(id, user)) return
        }
    }

    fun createAccount(userId: IDType, currencyName: String): Account {
        val account = accountService.createAccount(currencyName)
        do {
            val oldUser = getUser(userId)
            val newUser = oldUser.addAccount(account.id)
        } while (!users.replace(userId, oldUser, newUser))
        return account
    }

    fun deleteAccount(userId: IDType, accountId: IDType) {
        while (true) {
            val oldUser = getUser(userId)
            if (!oldUser.accounts.contains(accountId))
                throw IdNotFoundException("User with given id: $userId doesn't have an account with id: $accountId")

            val newUser = oldUser.removeAccount(accountId)

            val result = users.compute(userId) { _, user ->
                if (user !== oldUser) {
                    user
                } else {
                    accountService.deleteAccount(accountId)
                    newUser
                }
            }

            if (result === newUser) return
        }
    }
}