package services

import CreateNotAllowedException
import Currency
import ExchangeRateType
import IdNotFoundException
import java.util.concurrent.ConcurrentHashMap

final class CurrencyService {
    private val currencies = ConcurrentHashMap<String, Currency>()

    fun getCurrencies(): List<Currency> {
        return currencies.values.toList()
    }

    fun getCurrency(name: String): Currency {
        return currencies[name]
            ?: throw IdNotFoundException("Currency with the given name: \"$name\" doesn't exist")
    }

    fun createCurrency(name: String, exchangeRate: ExchangeRateType) {
        val currency = Currency(name, exchangeRate)
        val oldValue = currencies.putIfAbsent(name, currency)
        if (oldValue != null) {
            throw CreateNotAllowedException("Currency with the given name: \"$name\" already exists")
        }
    }

    fun changeCurrency(name: String, exchangeRate: ExchangeRateType) {
        currencies.computeIfPresent(name) { _, _ -> Currency(name, exchangeRate) }
            ?: throw IdNotFoundException("Currency with the given name: \"$name\" doesn't exist")
    }

    fun deleteCurrency(name: String) {
        currencies.remove(name)
            ?: throw IdNotFoundException("Currency with the given name: \"$name\" doesn't exist")
    }
}