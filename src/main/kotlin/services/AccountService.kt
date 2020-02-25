package services

import types.*
import errors.*
import entities.Account
import entities.AccountDTO
import entities.releaseLockAfter
import java.util.concurrent.locks.ReentrantLock

class AccountService(private val currencyService: CurrencyService) {
    private val accounts = HashMap<IDType, Account>()
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
     * Returns a snapshot of the given account.
     *
     * Acquires the account's lock.
     * Now that the account can't be modified, converts it to DTO and releases the acquired lock.
     *
     * @throws IdNotFoundException if account with the specified [id] doesn't exist.
     */
    fun getAccount(id: IDType): AccountDTO {
        val account = acquireAccountLock(id)
        return account.releaseLockAfter { account.toDTO() }
    }

    /**
     * Acquires lock of the account with the given [id] and returns that account.
     *
     * The lock associated with the returned account must be released at call sites.
     *
     * @throws IdNotFoundException if account with the specified [id] doesn't exist.
     */
    private fun acquireAccountLock(id: IDType): Account {
        return locked { accounts[id]?.apply(Account::acquireLock) } ?: throwAccountNotFound(id)
    }

    /**
     * Acquires lock of each account with id in [ids] and returns those accounts.
     *
     * Locks are acquired in natural order of their ids,
     * and the resulting list is in that same order.
     *
     * Locks associated with the returned accounts must be released at call sites.
     *
     * @throws IdNotFoundException if account with at least one of the specified [ids] doesn't exist.
     */
    fun acquireAccountLock(vararg ids: IDType): List<Account> {
        return locked {
            val result = mutableListOf<Account>()

            for (id in ids.sorted()) {
                val account = accounts[id]?.apply(Account::acquireLock)

                if (account == null) {
                    result.forEach(Account::releaseLock)
                    throwAccountNotFound(id)
                }

                result.add(account)
            }

            result
        }
    }

    /**
     * Creates and stores an account with the specified [currencyName], and returns the account's snapshot.
     *
     * @return the snapshot of the account created.
     * @throws IdNotFoundException if currency with the specified [currencyName] doesn't exist.
     */
    fun createAccount(currencyName: String): AccountDTO {
        val account = createAccountAndAcquireItsLock(currencyName)
        return account.releaseLockAfter { account.toDTO() }
    }

    /**
     * Creates, stores and returns an account with the specified [currencyName].
     * The lock of the returned account is already acquired by this thread.
     *
     * Used to prevent other threads modifying the created account after it is added to storage and
     * gets visible from other thread.
     *
     * Acquires the storage lock.
     * Creates an account and acquires its lock.
     * Adds the creates account to the storage.
     * Releases the storage lock.
     *
     * The lock associated with the created account must be released at call sites.
     *
     * @throws IdNotFoundException if currency with the specified [currencyName] doesn't exist.
     */
    fun createAccountAndAcquireItsLock(currencyName: String): Account {
        if (!currencyService.hasCurrency(currencyName))
            throwCurrencyNotFound(currencyName)

        return locked {
            val id = idGenerator.nextSuitable { !accounts.containsKey(it) }
            val account = Account(id, ZeroMoney, currencyName).apply(Account::acquireLock)
            accounts[id] = account
            account
        }
    }

    /**
     * Deletes the account with the given [id].
     *
     * Acquires the storage lock.
     * Acquires the lock of the account to be deleted.
     * Deletes the account from storage.
     * Releases the lock of the deleted account.
     * Releases the storage lock.
     *
     * @throws IdNotFoundException if there is no account stored with the specified [id].
     * @throws DeleteNotAllowedException if the account has non-zero balance.
     */
    fun deleteAccount(id: IDType) {
        locked {
            val account = acquireAccountLock(id)

            account.releaseLockAfter {
                if (!account.isEmpty())
                    throw DeleteNotAllowedException("The account has non-zero balance: ${account.balance}. Firstly withdraw all money from it")

                accounts.remove(id)
            }
        }
    }

    /**
     * Deposits the specified [amount] of money expressed in the specified [currencyName]
     * into account with the given [id].
     *
     * @throws IdNotFoundException if there is no currency with the given [currencyName].
     */
    fun depositIntoAccount(id: IDType, amount: MoneyType, currencyName: String) {
        require(amount > 0)
        updateAccountBalance(id, amount, currencyName)
    }

    /**
     * Withdraws the specified [amount] units of money expressed in the specified [currencyName]
     * from account with the given [id].
     *
     * @throws IdNotFoundException if there is no currency with the given [currencyName].
     * @throws BadRequest if the account has less money than the [amount] [currencyName].
     */
    fun withdrawFromAccount(id: IDType, amount: MoneyType, currencyName: String) {
        require(amount > 0)
        updateAccountBalance(id, -amount, currencyName)
    }

    /**
     * Updates balance of account with the specified [id],
     * putting [diff] units of money expressed in the specified [currencyName].
     *
     * Negative [diff] means withdraw, while positive [diff] means deposit.
     *
     * Acquires lock of the account with the given [id].
     * Acquires the account's currency and the given transaction currency in specific order. (see [acquireCurrencyLock])
     * Updates account balance.
     * Releases the account lock.
     * Releases the both currencies locks.
     *
     * @throws IdNotFoundException if there is no account with specified [id]
     * @throws IdNotFoundException if there is no currency with specified [currencyName]
     * @throws BadRequest if the account has less money than the -[diff] [currencyName]
     */
    private fun updateAccountBalance(id: IDType, diff: MoneyType, currencyName: String) {
        val account = acquireAccountLock(id)

        account.releaseLockAfter {
            val currencies = currencyService.acquireCurrencyLock(account.currencyName, currencyName)

            currencies.releaseLockAfter {
                val accountCurrency = currencies.first { it.name == account.currencyName }
                val transferCurrency = currencies.first { it.name == currencyName }

                val convertedDiff = transferCurrency.convert(diff, accountCurrency)

                if (account.balance + convertedDiff < 0)
                    throw BadRequest("Account balance: ${account.balance}, trying to withdraw: ${-convertedDiff}")

                account.updateBalance(convertedDiff)
            }
        }
    }
}