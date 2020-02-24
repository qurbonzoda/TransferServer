package entities

import ExchangeRateType
import MoneyType
import kotlinx.serialization.Serializable

@Serializable
data class Currency(
    val name: String,
    val exchangeRate: ExchangeRateType
) {
    fun changeExchangeRate(newExchangeRate: ExchangeRateType) =
        Currency(name, newExchangeRate)
}