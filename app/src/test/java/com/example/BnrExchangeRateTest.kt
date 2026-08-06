package com.example

import com.example.data.service.BnrRateResult
import com.example.data.service.ExchangeRateService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class BnrExchangeRateTest {

    private lateinit var exchangeRateService: ExchangeRateService

    @Before
    fun setUp() {
        // Dummy DAO for isolated service test
        val mockDao = object : com.example.data.dao.ExchangeRateDao {
            override suspend fun getOfficialRateForDate(date: String): com.example.data.model.ExchangeRateEntity? = null
            override suspend fun getRateForDate(date: String): com.example.data.model.ExchangeRateEntity? = null
            override suspend fun insertRate(rate: com.example.data.model.ExchangeRateEntity) {}
            override suspend fun deleteUnverifiedRatesForDate(date: String) {}
            override suspend fun insertAllRates(rates: List<com.example.data.model.ExchangeRateEntity>) {}
        }
        exchangeRateService = ExchangeRateService(mockDao)
    }

    @Test
    fun testXmlParsingAndEurRateExtraction() {
        val sampleXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <DataSet xmlns="http://www.bnr.ro/xsd">
                <Body>
                    <Cube date="2026-08-05">
                        <Rate currency="EUR">4.9765</Rate>
                        <Rate currency="USD">4.5120</Rate>
                    </Cube>
                    <Cube date="2026-08-04">
                        <Rate currency="EUR">4.9750</Rate>
                    </Cube>
                </Body>
            </DataSet>
        """.trimIndent()

        val rates = exchangeRateService.parseBnrXmlContent(sampleXml)
        assertEquals(2, rates.size)
        assertEquals(4.9765, rates["2026-08-05"]!!, 0.0001)
        assertEquals(4.9750, rates["2026-08-04"]!!, 0.0001)
    }

    @Test
    fun testInvalidXmlHandling() {
        val invalidXml = "<DataSet><Body><Cube date='2026-08-05'><Rate currency='EUR'>INVALID_NUMBER</Rate></Cube></Body></DataSet>"
        val rates = exchangeRateService.parseBnrXmlContent(invalidXml)
        assertTrue(rates.isEmpty())
    }

    @Test
    fun testAmountEurCalculationAndRounding() {
        val amountRON = 1000.0
        val rate = 4.9765

        // amountEUR = amountRON / rate
        // 1000 / 4.9765 = 200.944438862... -> Rounded HALF_UP to 200.94
        val calculated = ExchangeRateService.calculateAmountEUR(amountRON, rate)
        assertEquals(200.94, calculated, 0.001)
    }

    @Test
    fun testWeekendAndHolidayFallbackLogic() {
        val sampleXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <DataSet xmlns="http://www.bnr.ro/xsd">
                <Body>
                    <Cube date="2026-07-31">
                        <Rate currency="EUR">4.9760</Rate>
                    </Cube>
                </Body>
            </DataSet>
        """.trimIndent()

        val ratesMap = exchangeRateService.parseBnrXmlContent(sampleXml)
        val requestedDate = "2026-08-02" // Sunday

        val validEntry = ratesMap.entries
            .filter { it.key <= requestedDate }
            .maxByOrNull { it.key }

        assertEquals("2026-07-31", validEntry?.key)
        assertEquals(4.9760, validEntry?.value!!, 0.0001)
    }

    @Test
    fun testNoFabricatedFallbackRateWhenNetworkFails() {
        // When network fails, getOfficialRate or fetchOfficialBnrRateFromNetwork must return rate = 0.0 and PENDING
        val result = exchangeRateService.fetchOfficialBnrRateFromNetwork("1900-01-01")
        assertEquals("PENDING", result.status)
        assertEquals(0.0, result.rate, 0.00001)
        assertNotEquals(4.9750, result.rate, 0.00001)
        assertNotEquals(4.98, result.rate, 0.00001)
    }
}
