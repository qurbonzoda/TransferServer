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

    fun convert(amount: MoneyType, toCurrency: Currency): MoneyType {
        return amount * toCurrency.exchangeRate / exchangeRate
    }
}