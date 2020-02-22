import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.text.DateFormat

fun main(args: Array<String>) {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            gson {
                setDateFormat(DateFormat.LONG)
                setPrettyPrinting()
            }
        }

        routing {
            get("/currencies") {
                call.respond("Get all currencies")
            }

            route("/currency") {
                get("{id}") {
                    call.respond("Get currency with id = ${call.parameters["id"]}")
                }

                post {
                    val currency = call.receive<Currency>()
                    call.respond("Post currency with parameters: $currency")
                }

                put {
                    val currency = call.receive<CurrencyWithId>()
                    call.respond("Update exchange rate of currency with id = ${currency.id} to exchangeRate = ${currency.exchangeRate}")
                }

                delete("{id}") {
                    call.respond("Delete currency with id = ${call.parameters["id"]}")
                }
            }

            get("/transfers/account/{id}") {
                // TODO: Add query parameter for number of last operations to return
                val id = call.parameters["id"]
                val offset = call.request.queryParameters["offset"]
                val limit = call.request.queryParameters["limit"]
                call.respond("Get all transfer operations performed by the account with id = $id, offset: $offset, limit: $limit")
            }

            route("/transfer") {
                get("{id}") {
                    call.respond("Get transfer with id = ${call.parameters["id"]}")
                }

                post {
                    val transfer = call.receive<Transfer>()
                    call.respond("Post transfer with parameters: $transfer")
                }
            }

            route("/account") {
                get("{id}") {
                    call.respond("Get account with id = ${call.parameters["id"]}")
                }

                // e.g. from ATM
                put {
                    val diff = call.receive<BalanceDiff>()
                    call.respond("Put account diff: $diff")
                }
            }

            route("/user") {
                get("{id}") {
                    call.respond("Get user with id = ${call.parameters["id"]}")
                }

                put { // maybe put("{id}")
                    val change = call.receive<FullNameChange>()
                    call.respond("Change user full name: $change")
                }

                route("account") {
                    post {
                        val create = call.receive<CreateAccount>()
                        call.respond("Create account with currency id: $create")
                        // return id of the created account
                    }

                    delete("{id}") {
                        call.respond("Delete account with id: ${call.parameters["id"]}")
                    }
                }
            }
        }
    }.start()
}

data class Currency(val name: String, val exchangeRate: Double)
data class CurrencyWithId(val id: Int, val exchangeRate: Double)

data class Transfer(val fromAccountId: Int, val toAccountId: Int, val balance: Double, val currencyId: Int)

data class BalanceDiff(val id: Int, val diff: Double, val currencyId: Int)

data class FullNameChange(val id: Int, val newFullName: String)

data class CreateAccount(val currencyId: Int)
