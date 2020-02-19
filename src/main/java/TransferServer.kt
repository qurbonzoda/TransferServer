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
                    call.respond("Post currency with parameters: name = ${currency.name}, exchangeRate = ${currency.exchangeRate}")
                }

                put {
                    val currency = call.receive<CurrencyWithId>()
                    call.respond("Update exchange rate of currency with id = ${currency.id} to exchangeRate = ${currency.exchangeRate}")
                }

                delete("{id}") {
                    call.respond("Delete currency with id = ${call.parameters["id"]}")
                }
            }
        }
    }.start()
}

data class Currency(val name: String, val exchangeRate: Double)
data class CurrencyWithId(val id: Int, val exchangeRate: Double)