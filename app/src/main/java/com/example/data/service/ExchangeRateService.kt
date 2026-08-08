package com.example.data.service

import com.example.data.dao.ExchangeRateDao
import com.example.data.model.ExchangeRateEntity
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element

data class BnrRateResult(
    val requestedDate: String,
    val effectiveDate: String,
    val rate: Double,
    val source: String,
    val status: String // "OFFICIAL", "INVALID_DATE", "NO_NETWORK", "HTTP_ERROR", "XML_ERROR", "NOT_YET_PUBLISHED", "NO_APPLICABLE_RATE"
)

class ExchangeRateService(
    private val exchangeRateDao: ExchangeRateDao,
    private val httpFetcher: (String) -> Pair<String?, String> = ExchangeRateService::fetchUrlWithStatus
) {

    /**
     * Retrieves official BNR EUR/RON exchange rate for the specified transaction date.
     * Checks local cache first for an OFFICIAL BNR rate.
     * If missing, fetches official BNR XML archive for the transaction year, locates
     * the rate for requestedDate or closest preceding publication date (weekend/holiday fallback).
     * Returns error/pending result if offline/network error without fabricating rates.
     */
    suspend fun getOfficialRate(requestedDate: String): BnrRateResult = withContext(Dispatchers.IO) {
        // Strict date validation
        val parsedDate = try {
            LocalDate.parse(requestedDate, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            return@withContext BnrRateResult(
                requestedDate = requestedDate,
                effectiveDate = requestedDate,
                rate = 0.0,
                source = "NONE",
                status = "INVALID_DATE"
            )
        }

        val today = LocalDate.now(ZoneId.systemDefault())
        if (parsedDate.isAfter(today)) {
            return@withContext BnrRateResult(
                requestedDate = requestedDate,
                effectiveDate = requestedDate,
                rate = 0.0,
                source = "NONE",
                status = "NOT_YET_PUBLISHED"
            )
        }

        // 1. Check local cache
        val cached = exchangeRateDao.getOfficialRateForDate(requestedDate)
        if (cached != null && cached.status == "OFFICIAL" && cached.rate > 0.0) {
            return@withContext BnrRateResult(
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

        remoteResult
    }

    suspend fun getHistoricalRate(dateString: String): Double {
        val res = getOfficialRate(dateString)
        return res.rate
    }

    /**
     * Fetches official BNR XML archive and extracts the EUR rate for requested date or closest preceding date.
     */
    suspend fun fetchOfficialBnrRateFromNetwork(requestedDate: String): BnrRateResult = withContext(Dispatchers.IO) {
        val parsedDate = try {
            LocalDate.parse(requestedDate, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            return@withContext BnrRateResult(requestedDate, requestedDate, 0.0, "NONE", "INVALID_DATE")
        }

        val today = LocalDate.now(ZoneId.systemDefault())
        if (parsedDate.isAfter(today)) {
            return@withContext BnrRateResult(requestedDate, requestedDate, 0.0, "NONE", "NOT_YET_PUBLISHED")
        }

        val year = parsedDate.year

        // 1. Attempt year-specific archive XML
        var (xmlContent, httpStatus) = httpFetcher("https://curs.bnr.ro/files/xml/years/nbrfxrates$year.xml")

        // 2. Fallback to 10-day XML or current XML if year file not yet created
        if (xmlContent == null) {
            val fallback10 = httpFetcher("https://curs.bnr.ro/nbrfxrates10days.xml")
            if (fallback10.first != null) {
                xmlContent = fallback10.first
            } else {
                val fallbackCurr = httpFetcher("https://curs.bnr.ro/nbrfxrates.xml")
                xmlContent = fallbackCurr.first
                if (xmlContent == null && fallbackCurr.second != "200") {
                    httpStatus = fallbackCurr.second
                }
            }
        }

        if (xmlContent == null) {
            val statusStr = if (httpStatus == "NO_NETWORK") "NO_NETWORK" else "HTTP_ERROR"
            return@withContext BnrRateResult(
                requestedDate = requestedDate,
                effectiveDate = requestedDate,
                rate = 0.0,
                source = "NONE",
                status = statusStr
            )
        }

        val (ratesMap, xmlSuccess) = parseBnrXmlContentWithStatus(xmlContent)
        if (!xmlSuccess) {
            return@withContext BnrRateResult(
                requestedDate = requestedDate,
                effectiveDate = requestedDate,
                rate = 0.0,
                source = "NONE",
                status = "XML_ERROR"
            )
        }

        // Locate date <= requestedDate
        var validEntry = ratesMap.entries
            .filter { it.key <= requestedDate }
            .maxByOrNull { it.key }

        if (validEntry != null) {
            return@withContext BnrRateResult(
                requestedDate = requestedDate,
                effectiveDate = validEntry.key,
                rate = validEntry.value,
                source = "BNR_OFFICIAL",
                status = "OFFICIAL"
            )
        }

        // Try previous year archive if requested date is early January before first BNR publication
        val (prevYearXml, _) = httpFetcher("https://curs.bnr.ro/files/xml/years/nbrfxrates${year - 1}.xml")
        if (prevYearXml != null) {
            val (prevMap, prevSuccess) = parseBnrXmlContentWithStatus(prevYearXml)
            if (prevSuccess) {
                val prevEntry = prevMap.entries
                    .filter { it.key <= requestedDate }
                    .maxByOrNull { it.key }

                if (prevEntry != null) {
                    return@withContext BnrRateResult(
                        requestedDate = requestedDate,
                        effectiveDate = prevEntry.key,
                        rate = prevEntry.value,
                        source = "BNR_OFFICIAL",
                        status = "OFFICIAL"
                    )
                }
            }
        }

        return@withContext BnrRateResult(
            requestedDate = requestedDate,
            effectiveDate = requestedDate,
            rate = 0.0,
            source = "NONE",
            status = "NO_APPLICABLE_RATE"
        )
    }

    fun parseBnrXmlStream(inputStream: InputStream): Map<String, Double> {
        return parseBnrXmlStreamWithStatus(inputStream).first
    }

    fun parseBnrXmlStreamWithStatus(inputStream: InputStream): Pair<Map<String, Double>, Boolean> {
        val map = mutableMapOf<String, Double>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true

            // Secure DocumentBuilderFactory hardening
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            } catch (ignored: Exception) {}
            try {
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            } catch (ignored: Exception) {}
            try {
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            } catch (ignored: Exception) {}
            try {
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            } catch (ignored: Exception) {}
            factory.isXIncludeAware = false
            factory.isExpandEntityReferences = false

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
            return Pair(map, true)
        } catch (e: Exception) {
            return Pair(emptyMap(), false)
        }
    }

    fun parseBnrXmlContent(xml: String): Map<String, Double> {
        return parseBnrXmlStream(xml.byteInputStream())
    }

    fun parseBnrXmlContentWithStatus(xml: String): Pair<Map<String, Double>, Boolean> {
        return parseBnrXmlStreamWithStatus(xml.byteInputStream())
    }

    companion object {
        fun fetchUrlWithStatus(urlString: String): Pair<String?, String> {
            return try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.requestMethod = "GET"
                val code = conn.responseCode
                if (code == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    Pair(text, "200")
                } else {
                    Pair(null, code.toString())
                }
            } catch (e: java.io.IOException) {
                Pair(null, "NO_NETWORK")
            } catch (e: Exception) {
                Pair(null, "HTTP_ERROR")
            }
        }

        fun calculateAmountEUR(amountRON: Double, rate: Double): Double {
            if (rate <= 0.0) return 0.0
            return BigDecimal.valueOf(amountRON)
                .divide(BigDecimal.valueOf(rate), 2, RoundingMode.HALF_UP)
                .toDouble()
        }
    }
}
