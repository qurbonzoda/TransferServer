package entities

import IDType
import kotlinx.serialization.Serializable

@Serializable
final class User(
    val id: IDType,
    val fullName: String,
    val accountIds: Set<IDType>
) {
    fun addAccount(accountId: IDType): User {
        return User(id, fullName, accountIds + accountId)
    }

    fun removeAccount(accountId: IDType): User {
        return User(id, fullName, accountIds - accountId)
    }

    fun changeFullName(newFullName: String): User {
        return User(id, newFullName, accountIds)
    }
}