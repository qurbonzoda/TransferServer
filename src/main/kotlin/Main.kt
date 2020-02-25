import controllers.*
import errors.BadRequest
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.serialization.serialization
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import services.*

fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8080) {
        install(CallLogging)
        install(ContentNegotiation) {
            serialization()
        }
        install(StatusPages) {
            exception<BadRequest> {
                call.respond(HttpStatusCode.BadRequest, it.message.toString())
            }
        }

        val currencyService = CurrencyService()
        val accountService = AccountService(currencyService)
        val userService = UserService(accountService)
        val transferService = TransferService(accountService, currencyService)

        routing {
            apiCurrency(currencyService)
            apiAccount(accountService)
            apiUser(userService)
            apiTransfer(transferService)
        }
    }.start()
}
