class BadRequest(message: String?): Throwable(message)

class IdNotFoundException(message: String?): IllegalArgumentException(message)
class DeleteNotAllowedException(message: String?): IllegalStateException(message)
class CreateNotAllowedException(message: String?): IllegalArgumentException(message)