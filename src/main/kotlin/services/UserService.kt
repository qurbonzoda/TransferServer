package services

import entities.AccountDTO
import entities.User
import entities.UserDTO
import errors.DeleteNotAllowedException
import errors.IdNotFoundException
import errors.throwUserNotFound
import types.IDType
import java.util.concurrent.locks.ReentrantLock

class UserService(private val accountService: AccountService) {
    private val users = HashMap<IDType, User>()
    private val idGenerator = IdGenerator()
    private val lock = ReentrantLock()

    /**
     * Acquires the storage lock.
     * Executes the given [block].
     * Releases the storage lock.
     */
    private inline fun <R> locked(block: () -> R): R {
        lock.lock()
        return try { block() } finally { lock.unlock() }
    }

    /**
     * Returns a snapshot of the given user.
     *
     * Acquires the user's lock.
     * Now that the user can't be modified, converts it to DTO and releases the acquired lock.
     *
     * @throws IdNotFoundException if user with the specified [id] doesn't exist.
     */
    fun getUser(id: IDType): UserDTO {
        val user = acquireUserLock(id)
        return user.releaseLockAfter { user.toDTO() }
    }

    /**
     * Acquires lock of the user with the given [id] and returns that user.
     *
     * The lock associated with the returned user must be released at call sites.
     *
     * @throws IdNotFoundException if user with the specified [id] doesn't exist.
     */
    private fun acquireUserLock(id: IDType): User {
        return locked { users[id]?.apply(User::acquireLock) } ?: throwUserNotFound(id)
    }

    /**
     * Creates and stores a user with the specified [fullName], and returns the user's snapshot.
     *
     * @return the snapshot of the user created.
     */
    fun createUser(fullName: String): UserDTO {
        return locked {
            val id = idGenerator.nextSuitable { !users.containsKey(it) }
            val user = User(id, fullName, mutableSetOf())
            users[id] = user
            user.toDTO()
        }
    }

    /**
     * Changes full name of the user with the specified [id] to [newFullName].
     *
     * Acquires the user's lock.
     * Changes user's full name.
     * Releases the lock.
     *
     * @throws IdNotFoundException if user this the specified [id] doesn't exist.
     */
    fun updateUser(id: IDType, newFullName: String) {
        val user = acquireUserLock(id)
        user.releaseLockAfter { user.fullName = newFullName }
    }

    /**
     * Deletes the user with the given [id].
     *
     * Acquires the storage lock.
     * Acquires the lock of the user to be deleted.
     * Checks if the user has zero accounts.
     * Releases the lock of the user.
     * Deletes the user from storage.
     * Releases the storage lock.
     *
     * @throws IdNotFoundException if there is no user stored with the specified [id].
     * @throws DeleteNotAllowedException if the user has any accounts.
     */
    fun deleteUser(id: IDType) {
        locked {
            val user = acquireUserLock(id)

            user.releaseLockAfter {
                val accountCount = user.accountIds.size
                if (accountCount != 0)
                    throw DeleteNotAllowedException("The given user has $accountCount account(s). Firstly remove the user's all accounts")
            }

            users.remove(id)
        }
    }

    /**
     * Creates, stores and returns snapshot of an account with the specified [currencyName],
     * associated with the specified user.
     *
     * Acquires the user's lock.
     * Creates an account with the specified [currencyName].
     * Acquires the account's lock.
     * Adds the account to the accounts storage.
     * Adds the account id to the users [User.accountIds].
     * Converts the account to DTO.
     * Releases the account's lock.
     * Releases the user's lock.
     *
     * @return the snapshot of the account created.
     * @throws IdNotFoundException if user with the specified [userId] doesn't exist.
     * @throws IdNotFoundException if currency with the specified [currencyName] doesn't exist.
     */
    fun createAccount(userId: IDType, currencyName: String): AccountDTO {
        val user = acquireUserLock(userId)

        return user.releaseLockAfter {
            val account = accountService.createAccountAndAcquireItsLock(currencyName)

            account.releaseLockAfter {
                user.accountIds.add(account.id)
                account.toDTO()
            }
        }
    }

    /**
     * Deletes the account with the given [accountId] that is associated with user with the specified [userId].
     *
     * Acquires the user lock.
     * Checks if account belongs to
     * Deletes the account from storage.
     * Deletes the account from users [User.accountIds].
     * Releases the user lock.
     *
     * @throws IdNotFoundException if there is no user stored with the specified [userId].
     * @throws IdNotFoundException if there is no account stored with the specified [accountId].
     * @throws IdNotFoundException if there is no such account associated with the specified user.
     * @throws DeleteNotAllowedException if the account has non-zero balance.
     */
    fun deleteAccount(userId: IDType, accountId: IDType) {
        val user = acquireUserLock(userId)

        user.releaseLockAfter {
            if (!user.accountIds.contains(accountId))
                throw IdNotFoundException("User with given id: $userId doesn't have an account with id: $accountId")

            accountService.deleteAccount(accountId) // may throw exception
            user.accountIds.remove(accountId)
        }
    }
}