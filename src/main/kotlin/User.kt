final class User(
    val id: IDType,
    val fullName: String,
    val accounts: List<IDType>
) {
    fun addAccount(accountId: IDType): User {
        return User(id, fullName, accounts + accountId)
    }

    fun removeAccount(accountId: IDType): User {
        return User(id, fullName, accounts - accountId)
    }

    fun changeFullName(newFullName: String): User {
        return User(id, newFullName, accounts)
    }
}