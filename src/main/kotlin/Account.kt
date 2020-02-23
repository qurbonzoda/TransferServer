class Account(
    val id: IDType,
    val balance: MoneyType,
    val currency: Currency
) {
    fun diff(diff: MoneyType): Account {
        return Account(id, balance + diff, currency)
    }
}