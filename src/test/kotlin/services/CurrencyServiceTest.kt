package services

import BadRequest
import entities.Currency
import kotlin.test.*
import kotlin.random.Random


class CurrencyServiceTest {
    @Test
    fun getCurrencies() {
        val service = CurrencyService()

        assertTrue(service.getCurrencies().isEmpty())

        val expected = mutableSetOf(
            Currency("RUB", 64.07),
            Currency("USD", 1.0),
            Currency("EUR", 0.92)
        )

        expected.forEach { service.createCurrency(it.name, it.exchangeRate) }
        assertEquals(expected, service.getCurrencies())

        expected.random()
            .also { expected.remove(it) }
            .let { it.changeExchangeRate(it.exchangeRate + 1.0) }
            .also { expected.add(it) }
            .also { service.changeCurrency(it.name, it.exchangeRate) }

        assertEquals(expected, service.getCurrencies())

        repeat(100) {
            val currency = Currency(it.toString(), Random.nextDouble())
            expected.add(currency)
            service.createCurrency(currency.name, currency.exchangeRate)
        }

        assertEquals(expected, service.getCurrencies())
    }

    @Test
    fun getCurrency() {
        val service = CurrencyService()

        assertFailsWith<BadRequest> { service.getCurrency("RUB") }

        val expected = mutableSetOf<Currency>()

        repeat(100) {
            val currency = Currency(it.toString(), Random.nextDouble())
            expected.add(currency)
            service.createCurrency(currency.name, currency.exchangeRate)
        }

        repeat(100) {
            val name = it.toString()
            assertEquals(
                expected.first { currency -> currency.name == name },
                service.getCurrency(name)
            )
        }

        assertFailsWith<BadRequest> { service.getCurrency("100") }
    }

    @Test
    fun createCurrency() {
        val service = CurrencyService()

        service.createCurrency("RUB", 64.07)
        assertFailsWith<BadRequest> { service.createCurrency("RUB", 65.0) }

        service.createCurrency("EUR", 0.92)
        assertFailsWith<BadRequest> { service.createCurrency("EUR", 0.92) }

        assertEquals(
            setOf(Currency("RUB", 64.07), Currency("EUR", 0.92)),
            service.getCurrencies()
        )
    }

    @Test
    fun changeCurrency() {
        val service = CurrencyService()

        assertFailsWith<BadRequest> { service.changeCurrency("RUB", 65.0) }

        val expected = mutableSetOf<Currency>()

        repeat(100) {
            val currency = Currency(it.toString(), Random.nextDouble())
            expected.add(currency)
            service.createCurrency(currency.name, currency.exchangeRate)
        }

        repeat(100) {
            val name = it.toString()
            val newExchangeRate = Random.nextDouble()

            expected.removeIf { currency -> currency.name == name }
            expected.add(Currency(name, newExchangeRate))
            service.changeCurrency(name, newExchangeRate)
        }

        assertEquals(expected, service.getCurrencies())

        assertFailsWith<BadRequest> { service.changeCurrency("100", 65.0) }
    }

    @Test
    fun deleteCurrency() {
        val service = CurrencyService()

        assertFailsWith<BadRequest> { service.deleteCurrency("RUB") }

        val expected = mutableSetOf<Currency>()

        repeat(100) {
            val currency = Currency(it.toString(), Random.nextDouble())
            expected.add(currency)
            service.createCurrency(currency.name, currency.exchangeRate)
        }

        repeat(100) {
            val name = it.toString()
            expected.removeIf { currency -> currency.name == name }
            service.deleteCurrency(name)

            assertFailsWith<BadRequest> { service.deleteCurrency(name) }
        }

        assertTrue(service.getCurrencies().isEmpty())
    }
}