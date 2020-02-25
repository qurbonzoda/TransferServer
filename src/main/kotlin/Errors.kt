package errors

import types.IDType

open class BadRequest(message: String?): Throwable(message)

class IdNotFoundException(message: String?): BadRequest(message)
class DeleteNotAllowedException(message: String?): BadRequest(message)
class CreateNotAllowedException(message: String?): BadRequest(message)


fun throwCurrencyNotFound(name: String): Nothing {
    throw IdNotFoundException("Currency with the given name: \"$name\" doesn't exist")
}

fun throwCurrencyAlreadyExists(name: String): Nothing {
    throw CreateNotAllowedException("Currency with the given name: \"$name\" already exists")
}

fun throwAccountNotFound(id: IDType): Nothing {
    throw IdNotFoundException("Account with the given id: \"$id\" doesn't exist")
}

fun throwUserNotFound(id: IDType): Nothing {
    throw IdNotFoundException("User with the given id: \"$id\" doesn't exist")
}

fun throwTransferNotFound(id: IDType): Nothing {
    throw IdNotFoundException("Transfer with the given id: \"$id\" doesn't exist")
}