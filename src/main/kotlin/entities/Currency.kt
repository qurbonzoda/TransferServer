package entities

import kotlinx.serialization.Serializable
import types.ExchangeRateType
import types.MoneyType
import java.util.concurrent.locks.ReentrantLock

class Currency(
    val name: String,
    var exchangeRate: ExchangeRateType
) {
    private val lock = ReentrantLock()

    fun acquireLock() = lock.lock()
    fun releaseLock() = lock.unlock()

    inline fun <R> releaseLockAfter(block: () -> R): R {
        return try { block() } finally { releaseLock() }
    }

    fun updateExchangeRate(newExchangeRate: ExchangeRateType) {
        check(lock.isHeldByCurrentThread)
        exchangeRate = newExchangeRate
    }

    fun convert(amount: MoneyType, toCurrency: Currency): MoneyType {
        check(lock.isHeldByCurrentThread)
        check(toCurrency.lock.isHeldByCurrentThread)
        return amount * toCurrency.exchangeRate / exchangeRate
    }

    fun toDTO(): CurrencyDTO {
        check(lock.isHeldByCurrentThread)
        return CurrencyDTO(name, exchangeRate)
    }
}

fun <R> List<Currency>.releaseLockAfter(block: () -> R): R {
    if (isEmpty())
        return block()

    return first().releaseLockAfter { drop(1).releaseLockAfter(block) }
}

@Serializable
data class CurrencyDTO(
    val name: String,
    val exchangeRate: ExchangeRateType
) {
    // For tests
    fun convert(amount: MoneyType, toCurrency: CurrencyDTO): MoneyType {
        return amount * toCurrency.exchangeRate / exchangeRate
    }
}