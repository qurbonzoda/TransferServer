package services

import CreateNotAllowedException
import Currency
import ExchangeRateType
import IdNotFoundException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

final class CurrencyService {
    private val currencies = HashMap<String, Currency>()
    private val lock = ReentrantLock()

    fun getCurrencies(): List<Currency> {
        acquireLock()
        val result = currencies.values.toList()
        releaseLock()

        return result
    }

    fun getCurrency(name: String): Currency {
        acquireLock()
        val result = currencies[name]
        releaseLock()

        return result ?: throw IdNotFoundException("Currency with the given name: \"$name\" doesn't exist")
    }

    fun createCurrency(name: String, exchangeRate: ExchangeRateType) {
        val currency = Currency(name, exchangeRate)

        acquireLock()
        val oldValue = currencies.putIfAbsent(name, currency)
        releaseLock()

        if (oldValue != null) throw CreateNotAllowedException("Currency with the given name: \"$name\" already exists")
    }

    fun changeCurrency(name: String, exchangeRate: ExchangeRateType) {
        acquireLock()
        val oldValue = currencies.computeIfPresent(name) { _, _ -> Currency(name, exchangeRate) }
        releaseLock()

        if (oldValue == null) throw IdNotFoundException("Currency with the given name: \"$name\" doesn't exist")
    }

    fun deleteCurrency(name: String) {
        acquireLock()
        val oldValue = currencies.remove(name)
        releaseLock()

        if (oldValue == null) throw IdNotFoundException("Currency with the given name: \"$name\" doesn't exist")
    }

    fun acquireLock() {
        lock.lock()
    }

    fun releaseLock() {
        lock.unlock()
    }
}