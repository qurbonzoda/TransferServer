open class BadRequest(message: String?): Throwable(message)

class IdNotFoundException(message: String?): BadRequest(message)
class DeleteNotAllowedException(message: String?): BadRequest(message)
class CreateNotAllowedException(message: String?): BadRequest(message)