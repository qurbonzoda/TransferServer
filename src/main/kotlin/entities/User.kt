package entities

import types.IDType
import kotlinx.serialization.Serializable
import java.util.concurrent.locks.ReentrantLock

class User(
    val id: IDType,
    var fullName: String,
    val accountIds: MutableSet<IDType>
) {
    private val lock = ReentrantLock()

    fun acquireLock() = lock.lock()
    fun releaseLock() = lock.unlock()

    inline fun <R> releaseLockAfter(block: () -> R): R {
        return try { block() } finally { releaseLock() }
    }

    fun toDTO(): UserDTO {
        return UserDTO(id, fullName, accountIds.toMutableSet())
    }
}

@Serializable
data class UserDTO(
    val id: IDType,
    val fullName: String,
    val accountIds: Set<IDType>
)