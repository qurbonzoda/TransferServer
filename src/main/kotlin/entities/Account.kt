package entities

import IDType
import MoneyType
import kotlinx.serialization.Serializable

@Serializable
class Account(
    val id: IDType,
    val balance: MoneyType,
    val currencyName: String
) {
    fun diff(diff: MoneyType): Account {
        return Account(id, balance + diff, currencyName)
    }
}