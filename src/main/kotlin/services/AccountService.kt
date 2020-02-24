package services

import entities.Account
import DeleteNotAllowedException
import IDType
import IdNotFoundException
import MoneyType
import java.util.concurrent.ConcurrentHashMap

final class AccountService(private val currencyService: CurrencyService) {
    private val accounts = ConcurrentHashMap<IDType, Account>()
    private val idGenerator = IdGenerator()

    fun getAccount(id: IDType): Account {
        return accounts[id]
            ?: throw IdNotFoundException("Account with the given id: \"$id\" doesn't exist")
    }

    fun createAccount(currencyName: String): Account {
        while (true) {
            val id = idGenerator.next()
            val account = Account(id, 0.0, currencyName)
            accounts.putIfAbsent(id, account) ?: return account
        }
    }

    fun deleteAccount(id: IDType) {
        do {
            val account = getAccount(id)
            if (account.balance != 0.0)
                throw DeleteNotAllowedException("The account has non-zero balance: ${account.balance}. Firstly withdraw all money from it")
        } while (!accounts.remove(id, account))
    }

    fun depositIntoAccount(id: IDType, amount: MoneyType, currencyName: String) {
        require(amount > 0)
        changeAccountBalance(id, amount, currencyName)
    }

    fun withdrawFromAccount(id: IDType, amount: MoneyType, currencyName: String) {
        require(amount > 0)
        changeAccountBalance(id, -amount, currencyName)
    }

    private fun changeAccountBalance(id: IDType, diff: MoneyType, currencyName: String) {
//        val currency = currencyService.getCurrency(currencyName)

        // TODO: convert the given amount to the right currency.
        accounts.computeIfPresent(id) { _, old -> old.diff(diff) }
            ?: throw IdNotFoundException("Account with the given id: \"$id\" doesn't exist")
    }

    fun acquireLock(id: IDType, remappingFunction: (IDType, Account) -> Account): Account? {
        return accounts.computeIfPresent(id, remappingFunction)
    }
}