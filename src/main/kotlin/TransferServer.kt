import controllers.apiAccount
import controllers.apiCurrency
import controllers.apiTransfer
import controllers.apiUser
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import services.AccountService
import services.CurrencyService
import services.TransferService
import services.UserService
import java.text.DateFormat

fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            gson {
                setDateFormat(DateFormat.LONG)
                setPrettyPrinting()
            }
        }
        install(StatusPages) {
            exception<BadRequest> {
                call.respond(HttpStatusCode.BadRequest, it.message.toString())
            }
            exception<IdNotFoundException> {
                call.respond(HttpStatusCode.BadRequest, it.message.toString())
            }
            exception<DeleteNotAllowedException> {
                call.respond(HttpStatusCode.BadRequest, it.message.toString())
            }
            exception<CreateNotAllowedException> {
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
