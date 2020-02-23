package services

import IDType
import java.util.concurrent.atomic.AtomicInteger

final class IdGenerator {
    private val nextId = AtomicInteger(0)

    fun next(): IDType {
        return nextId.getAndIncrement()
    }
}