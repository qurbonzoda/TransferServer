package entities

import types.IDType
import types.MoneyType
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.absoluteValue
import kotlinx.serialization.Serializable

class Account(
    val id: IDType,
    var balance: MoneyType,
    val currencyName: String
) {
    private val lock = ReentrantLock()

    fun acquireLock() = lock.lock()
    fun releaseLock() = lock.unlock()

    inline fun <R> releaseLockAfter(block: () -> R): R {
        return try { block() } finally { releaseLock() }
    }

    fun updateBalance(diff: MoneyType) {
        check(lock.isHeldByCurrentThread)
        check(balance + diff >= 0)
        balance += diff
    }

    fun isEmpty(): Boolean {
        check(lock.isHeldByCurrentThread)
        return balance.absoluteValue < 1E-6
    }

    fun toDTO(): AccountDTO {
        check(lock.isHeldByCurrentThread)
        return AccountDTO(id, balance, currencyName)
    }
}

fun <R> List<Account>.releaseLockAfter(block: () -> R): R {
    if (isEmpty())
        return block()

    return first().releaseLockAfter { drop(1).releaseLockAfter(block) }
}


@Serializable
data class AccountDTO(
    val id: IDType,
    val balance: MoneyType,
    val currencyName: String
)