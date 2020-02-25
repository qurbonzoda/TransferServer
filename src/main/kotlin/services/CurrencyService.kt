package services

import entities.Currency
import entities.CurrencyDTO
import errors.*
import kotlinx.atomicfu.atomic
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import types.ExchangeRateType

class CurrencyService {
    private val currenciesRef = atomic(persistentListOf<Currency>())

    /**
     * Returns a snapshot of the currencies.
     *
     * Acquires each currency's lock in order specified by [lockingOrder].
     * Now that currencies can't be modified, converts them to DTOs and then releases all locks.
     */
    fun getCurrencies(): Set<CurrencyDTO> {
        val currencies = lockingOrder(currenciesRef.value)

        currencies.forEach(Currency::acquireLock)
        val currencyDTOs = currencies.map { it.toDTO() }
        currencies.forEach(Currency::releaseLock)

        return currencyDTOs.toSet()
    }

    /**
     * Checks if the given currency exists.
     *
     * Does not acquire currency's lock.
     *
     * As currencies are never deleted, though can be modified,
     * it is enough to check it once if existence is what you need.
     */
    fun hasCurrency(name: String): Boolean {
        return currenciesRef.value.indexOfCurrency(name) != -1
    }

    /**
     * Returns a snapshot of the given currency.
     *
     * Acquires the currency's lock.
     * Now that the currency can't modified, converts it to DTO and releases the acquired lock.
     *
     * @throws IdNotFoundException if currency with the specified [name] doesn't exist.
     */
    fun getCurrency(name: String): CurrencyDTO {
        val currency = acquireCurrencyLock(name)
        return currency.releaseLockAfter { currency.toDTO() }
    }

    /**
     * Acquires lock of the currency with the given [name] and returns that currency.
     *
     * The lock associated with the returned currency must be released at call sites.
     *
     * @throws IdNotFoundException if currency with the specified [name] doesn't exist.
     */
    private fun acquireCurrencyLock(name: String): Currency {
        val currency = currenciesRef.value.findCurrency(name) ?: throwCurrencyNotFound(name)
        return currency.apply(Currency::acquireLock)
    }

    /**
     * Acquires lock of each currency with name in [names] and returns those currencies.
     *
     * Locks are acquired in order specified by [lockingOrder],
     * and the resulting list is in the same [lockingOrder] order.
     *
     * Locks associated with the returned currencies must be released at call sites.
     *
     * @throws IdNotFoundException if currency with at least one of the specified [names] doesn't exist.
     */
    fun acquireCurrencyLock(vararg names: String): List<Currency> {
        val uniqueNames = names.toHashSet()
        val frozen = currenciesRef.value
        val currencies = uniqueNames.map { name ->
            frozen.findCurrency(name) ?: throwCurrencyNotFound(name)
        }
        return lockingOrder(currencies).apply { forEach(Currency::acquireLock) }
    }

    /**
     * Lock currencies in lexicographical order of their names.
     *
     * If you are to acquire locks of multiple currencies, acquire them always in the same order.
     */
    private fun lockingOrder(currencies: List<Currency>): List<Currency> {
        return currencies.sortedBy { it.name }
    }

    /**
     * Creates and stores a currency with the specified [name] and [exchangeRate].
     *
     * @throws CreateNotAllowedException if currency with the specified [name] already exists.
     */
    fun createCurrency(name: String, exchangeRate: ExchangeRateType) {
        val currency = Currency(name, exchangeRate)

        do {
            val currencies = currenciesRef.value
            if (currencies.indexOfCurrency(name) != -1) throwCurrencyAlreadyExists(name)
        } while (!currenciesRef.compareAndSet(currencies, currencies.add(currency)))
    }

    /**
     * Updates exchange rate of an existing currency with the specified [name].
     *
     * Acquires lock of the currency to be updated.
     * Updates exchange rate of the currency.
     * Releases the lock.
     *
     * @throws IdNotFoundException if there is no currency stored with the specified name.
     */
    fun updateCurrency(name: String, exchangeRate: ExchangeRateType) {
        val currency = acquireCurrencyLock(name)
        currency.releaseLockAfter { currency.updateExchangeRate(exchangeRate) }
    }

    // What to do with accounts that operate with the currency to delete?

//    /**
//     * Deletes the currency with the given [name].
//     *
//     * Acquires the lock of the currency to be deleted.
//     * Deletes the currency from storage.
//     * Releases the lock.
//     *
//     * @throws IdNotFoundException if there is no currency stored with the specified name.
//     */
//    fun deleteCurrency(name: String) {
//        val currency = acquireCurrencyLock(name) ?: throwCurrencyNotFound(name)
//        do {
//            val currencies = currenciesRef.value
//        } while (!currenciesRef.compareAndSet(currencies, currencies.remove(currency)))
//        currency.releaseLock()
//    }
}

/** Returns the index of currency with the specified name. */
private fun PersistentList<Currency>.indexOfCurrency(name: String) = this.indexOfFirst { it.name == name }
/** Returns the currency with the specified name. */
private fun PersistentList<Currency>.findCurrency(name: String) = this.find { it.name == name }