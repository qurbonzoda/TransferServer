package controllers

import ExchangeRateType
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import services.CurrencyService

fun Routing.apiCurrency(service: CurrencyService) {
    get("/currencies") {
        val currencies = service.getCurrencies()
        call.respond(currencies)
    }

    route("/currency") {
        get("{name}") {
            val name = call.parameters["name"]!!
            val currency = service.getCurrency(name)
            call.respond(currency)
        }

        post {
            val currency = call.receive<CurrencyDTO>()
            service.createCurrency(currency.name, currency.exchangeRate)
            call.respond(HttpStatusCode.OK)
        }

        put {
            val currency = call.receive<CurrencyDTO>()
            service.changeCurrency(currency.name, currency.exchangeRate)
            call.respond(HttpStatusCode.OK)
        }

        delete("{name}") {
            val name = call.parameters["name"]!!
            service.deleteCurrency(name)
            call.respond(HttpStatusCode.OK)
        }
    }
}

data class CurrencyDTO(
    val name: String,
    val exchangeRate: ExchangeRateType
)