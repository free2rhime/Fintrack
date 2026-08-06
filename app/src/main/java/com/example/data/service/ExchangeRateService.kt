package com.example.data.service

import com.example.data.dao.ExchangeRateDao
import com.example.data.model.ExchangeRateEntity
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

data class BnrRateResult(
    val requestedDate: String,
    val effectiveDate: String,
    val rate: Double,
    val source: String,
    val status: String
)

class ExchangeRateService(private val exchangeRateDao: ExchangeRateDao) {

    /**
     * Retrieves official BNR EUR/RON exchange rate for the specified transaction date.
     * Checks local cache first for an OFFICIAL BNR rate.
     * If missing, fetches official BNR XML archive for the transaction year, locates
     * the rate for requestedDate or closest preceding publication date (weekend/holiday fallback).
     * Returns PENDING if offline/network error without fabricating rates.
     */
    suspend fun getOfficialRate(requestedDate: String): BnrRateResult {
        // 1. Check local cache
        val cached = exchangeRateDao.getOfficialRateForDate(requestedDate)
        if (cached != null && cached.status == "OFFICIAL" && cached.rate > 0.0) {
            return BnrRateResult(
                requestedDate = requestedDate,
                effectiveDate = cached.effectiveDate,
                rate = cached.rate,
                source = cached.source,
                status = cached.status
            )
        }

        // 2. Fetch from official BNR XML endpoint
        val remoteResult = fetchOfficialBnrRateFromNetwork(requestedDate)

        // 3. Cache in database if official
        if (remoteResult.status == "OFFICIAL" && remoteResult.rate > 0.0) {
            val entity = ExchangeRateEntity(
                date = requestedDate,
                requestedDate = requestedDate,
                effectiveDate = remoteResult.effectiveDate,
                rate = remoteResult.rate,
                source = "BNR_OFFICIAL",
                fetchedAt = System.currentTimeMillis(),
                status = "OFFICIAL"
            )
            exchangeRateDao.insertRate(entity)
        }

        return remoteResult
    }

    /**
     * Backward-compatible helper returning Double rate or 0.0 if pending.
     */
    suspend fun getHistoricalRate(dateString: String): Double {
        val res = getOfficialRate(dateString)
        return res.rate
    }

    /**
     * Fetches official BNR XML archive and extracts the EUR rate for requested date or closest preceding date.
     */
    fun fetchOfficialBnrRateFromNetwork(requestedDate: String): BnrRateResult {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        if (requestedDate > todayStr) {
            return BnrRateResult(
                requestedDate = requestedDate,
                effectiveDate = requestedDate,
                rate = 0.0,
                source = "BNR_OFFICIAL",
                status = "PENDING"
            )
        }

        val yearStr = requestedDate.take(4)
        val year = yearStr.toIntOrNull() ?: 2026

        // 1. Attempt year-specific archive XML
        var xmlContent = fetchUrl("https://curs.bnr.ro/files/xml/years/nbrfxrates$year.xml")

        // 2. Fallback to 10-day XML or current XML if year file not yet created
        if (xmlContent == null) {
            xmlContent = fetchUrl("https://curs.bnr.ro/nbrfxrates10days.xml")
                ?: fetchUrl("https://curs.bnr.ro/nbrfxrates.xml")
        }

        var ratesMap = if (xmlContent != null) parseBnrXmlContent(xmlContent) else emptyMap()

        // Locate date <= requestedDate
        var validEntry = ratesMap.entries
            .filter { it.key <= requestedDate }
            .maxByOrNull { it.key }

        if (validEntry != null) {
            return BnrRateResult(
                requestedDate = requestedDate,
                effectiveDate = validEntry.key,
                rate = validEntry.value,
                source = "BNR_OFFICIAL",
                status = "OFFICIAL"
            )
        }

        // Try previous year archive if requested date is early January before first BNR publication
        val prevYearXml = fetchUrl("https://curs.bnr.ro/files/xml/years/nbrfxrates${year - 1}.xml")
        if (prevYearXml != null) {
            val prevMap = parseBnrXmlContent(prevYearXml)
            val prevEntry = prevMap.entries
                .filter { it.key <= requestedDate }
                .maxByOrNull { it.key }

            if (prevEntry != null) {
                return BnrRateResult(
                    requestedDate = requestedDate,
                    effectiveDate = prevEntry.key,
                    rate = prevEntry.value,
                    source = "BNR_OFFICIAL",
                    status = "OFFICIAL"
                )
            }
        }

        // Return PENDING status when no network or rate is available
        return BnrRateResult(
            requestedDate = requestedDate,
            effectiveDate = requestedDate,
            rate = 0.0,
            source = "BNR_OFFICIAL",
            status = "PENDING"
        )
    }

    fun parseBnrXmlStream(inputStream: InputStream): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(inputStream)
            val cubeList = doc.getElementsByTagName("Cube")

            for (i in 0 until cubeList.length) {
                val cubeElement = cubeList.item(i) as? Element ?: continue
                val cubeDate = cubeElement.getAttribute("date")
                if (cubeDate.isNullOrBlank()) continue

                val rateList = cubeElement.getElementsByTagName("Rate")
                for (j in 0 until rateList.length) {
                    val rateElement = rateList.item(j) as? Element ?: continue
                    val currency = rateElement.getAttribute("currency")
                    if (currency == "EUR") {
                        val rawText = rateElement.textContent?.trim() ?: continue
                        val parsedRate = rawText.toDoubleOrNull() ?: continue
                        val multiplierAttr = rateElement.getAttribute("multiplier")
                        val multiplier = multiplierAttr.toDoubleOrNull() ?: 1.0
                        map[cubeDate] = parsedRate / multiplier
                    }
                }
            }
        } catch (e: Exception) {
            // Silently handle XML parse errors
        }
        return map
    }

    fun parseBnrXmlContent(xml: String): Map<String, Double> {
        return parseBnrXmlStream(xml.byteInputStream())
    }

    private fun fetchUrl(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun calculateAmountEUR(amountRON: Double, rate: Double): Double {
            if (rate <= 0.0) return 0.0
            return BigDecimal.valueOf(amountRON)
                .divide(BigDecimal.valueOf(rate), 2, RoundingMode.HALF_UP)
                .toDouble()
        }
    }
}
