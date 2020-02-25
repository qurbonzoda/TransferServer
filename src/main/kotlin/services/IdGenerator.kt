package services

import types.IDType
import java.util.concurrent.atomic.AtomicInteger

class IdGenerator {
    private val nextId = AtomicInteger(0)

    fun next(): IDType {
        return nextId.getAndIncrement()
    }

    inline fun nextSuitable(predicate: (IDType) -> Boolean): IDType {
        while (true) {
            val id = next()
            if (predicate(id)) return id
        }
    }
}