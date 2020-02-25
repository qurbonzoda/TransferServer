package services

import errors.BadRequest
import entities.CurrencyDTO
import kotlin.test.*
import kotlin.random.Random


class CurrencyServiceTest {
    @Test
    fun getCurrencies() {
        val service = CurrencyService()

        assertTrue(service.getCurrencies().isEmpty())

        val expected = mutableSetOf(
            CurrencyDTO("RUB", 64.07),
            CurrencyDTO("USD", 1.0),
            CurrencyDTO("EUR", 0.92)
        )

        expected.forEach { service.createCurrency(it.name, it.exchangeRate) }
        assertEquals(expected, service.getCurrencies())

        expected.random()
            .also { expected.remove(it) }
            .let { CurrencyDTO(it.name, it.exchangeRate + 1.0) }
            .also { expected.add(it) }
            .also { service.updateCurrency(it.name, it.exchangeRate) }

        assertEquals(expected, service.getCurrencies())

        repeat(100) {
            val currency = CurrencyDTO(it.toString(), Random.nextDouble())
            expected.add(currency)
            service.createCurrency(currency.name, currency.exchangeRate)
        }

        assertEquals(expected, service.getCurrencies())
    }

    @Test
    fun getCurrency() {
        val service = CurrencyService()

        assertFailsWith<BadRequest> { service.getCurrency("RUB") }

        val expected = mutableSetOf<CurrencyDTO>()

        repeat(100) {
            val currency = CurrencyDTO(it.toString(), Random.nextDouble())
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
            setOf(CurrencyDTO("RUB", 64.07), CurrencyDTO("EUR", 0.92)),
            service.getCurrencies()
        )
    }

    @Test
    fun updateCurrency() {
        val service = CurrencyService()

        assertFailsWith<BadRequest> { service.updateCurrency("RUB", 65.0) }

        val expected = mutableSetOf<CurrencyDTO>()

        repeat(100) {
            val currency = CurrencyDTO(it.toString(), Random.nextDouble())
            expected.add(currency)
            service.createCurrency(currency.name, currency.exchangeRate)
        }

        repeat(100) {
            val name = it.toString()
            val newExchangeRate = Random.nextDouble()

            expected.removeIf { currency -> currency.name == name }
            expected.add(CurrencyDTO(name, newExchangeRate))
            service.updateCurrency(name, newExchangeRate)
        }

        assertEquals(expected, service.getCurrencies())

        assertFailsWith<BadRequest> { service.updateCurrency("100", 65.0) }
    }

//    @Test
//    fun deleteCurrency() {
//        val service = CurrencyService()
//
//        assertFailsWith<BadRequest> { service.deleteCurrency("RUB") }
//
//        val expected = mutableSetOf<Currency>()
//
//        repeat(100) {
//            val currency = Currency(it.toString(), Random.nextDouble())
//            expected.add(currency)
//            service.createCurrency(currency.name, currency.exchangeRate)
//        }
//
//        repeat(100) {
//            val name = it.toString()
//            expected.removeIf { currency -> currency.name == name }
//            service.deleteCurrency(name)
//
//            assertFailsWith<BadRequest> { service.deleteCurrency(name) }
//        }
//
//        assertTrue(service.getCurrencies().isEmpty())
//    }
}