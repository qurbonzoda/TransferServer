package entities

import ExchangeRateType

final data class Currency(
    val name: String,
    val exchangeRate: ExchangeRateType
) {
    fun changeExchangeRate(newExchangeRate: ExchangeRateType) =
        Currency(name, newExchangeRate)
}