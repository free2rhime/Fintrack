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
    val status: String, // "OFFICIAL", "NO_INTERNET_PERMISSION", "NO_NETWORK", "DNS_FAILURE", "TLS_FAILURE", "TIMEOUT", "HTTP_ERROR", "EMPTY_RESPONSE", "XML_PARSE_ERROR", "EUR_RATE_NOT_FOUND", "NO_APPLICABLE_DATE", "NOT_YET_PUBLISHED", "INVALID_DATE"
    val diagnostic: String? = null
)

data class BnrDiagnosticResult(
    val isReachable: Boolean,
    val httpStatus: String,
    val publicationDatesParsed: Int,
    val eurRateFound: Boolean,
    val latestPublicationDate: String?
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
                status = "INVALID_DATE",
                diagnostic = "Invalid date format: $requestedDate"
            )
        }

        val today = LocalDate.now(ZoneId.systemDefault())
        if (parsedDate.isAfter(today)) {
            return@withContext BnrRateResult(
                requestedDate = requestedDate,
                effectiveDate = requestedDate,
                rate = 0.0,
                source = "NONE",
                status = "NOT_YET_PUBLISHED",
                diagnostic = "Requested date $requestedDate is in the future relative to today $today"
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
                status = cached.status,
                diagnostic = "Cache hit: Requested $requestedDate, Effective ${cached.effectiveDate}, Rate ${cached.rate}"
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
            return@withContext BnrRateResult(requestedDate, requestedDate, 0.0, "NONE", "INVALID_DATE", "Invalid date format: $requestedDate")
        }

        val today = LocalDate.now(ZoneId.systemDefault())
        if (parsedDate.isAfter(today)) {
            return@withContext BnrRateResult(requestedDate, requestedDate, 0.0, "NONE", "NOT_YET_PUBLISHED", "Future date: $requestedDate")
        }

        val year = parsedDate.year

        // Try primary year endpoint
        val yearUrl = "https://curs.bnr.ro/files/xml/years/nbrfxrates$year.xml"
        var (xmlContent, httpStatus) = httpFetcher(yearUrl)
        var selectedEndpoint = yearUrl

        // Fallback to 10-day XML or current XML if year file not yet created or HTTP error
        if (xmlContent == null) {
            val fallback10Url = "https://curs.bnr.ro/nbrfxrates10days.xml"
            val fallback10 = httpFetcher(fallback10Url)
            if (fallback10.first != null) {
                xmlContent = fallback10.first
                httpStatus = fallback10.second
                selectedEndpoint = fallback10Url
            } else {
                val fallbackCurrUrl = "https://curs.bnr.ro/nbrfxrates.xml"
                val fallbackCurr = httpFetcher(fallbackCurrUrl)
                xmlContent = fallbackCurr.first
                if (xmlContent != null) {
                    httpStatus = fallbackCurr.second
                    selectedEndpoint = fallbackCurrUrl
                } else {
                    if (httpStatus == "200") {
                        httpStatus = fallbackCurr.second
                    }
                }
            }
        }

        if (xmlContent == null) {
            val failureCategory = if (httpStatus in listOf(
                    "NO_INTERNET_PERMISSION", "NO_NETWORK", "DNS_FAILURE", "TLS_FAILURE", "TIMEOUT", "EMPTY_RESPONSE"
                )
            ) httpStatus else "HTTP_ERROR"

            val diagnosticMsg = "RequestedDate=$requestedDate, Endpoint=$selectedEndpoint, HttpStatus=$httpStatus, Category=$failureCategory"
            logDebugDiagnostic(diagnosticMsg)

            return@withContext BnrRateResult(
                requestedDate = requestedDate,
                effectiveDate = requestedDate,
                rate = 0.0,
                source = "NONE",
                status = failureCategory,
                diagnostic = diagnosticMsg
            )
        }

        val (ratesMap, xmlSuccess) = parseBnrXmlContentWithStatus(xmlContent)
        if (!xmlSuccess) {
            val diagnosticMsg = "RequestedDate=$requestedDate, Endpoint=$selectedEndpoint, HttpStatus=$httpStatus, Category=XML_PARSE_ERROR"
            logDebugDiagnostic(diagnosticMsg)
            return@withContext BnrRateResult(
                requestedDate = requestedDate,
                effectiveDate = requestedDate,
                rate = 0.0,
                source = "NONE",
                status = "XML_PARSE_ERROR",
                diagnostic = diagnosticMsg
            )
        }

        if (ratesMap.isEmpty()) {
            val diagnosticMsg = "RequestedDate=$requestedDate, Endpoint=$selectedEndpoint, HttpStatus=$httpStatus, Category=EUR_RATE_NOT_FOUND"
            logDebugDiagnostic(diagnosticMsg)
            return@withContext BnrRateResult(
                requestedDate = requestedDate,
                effectiveDate = requestedDate,
                rate = 0.0,
                source = "NONE",
                status = "EUR_RATE_NOT_FOUND",
                diagnostic = diagnosticMsg
            )
        }

        // Locate maximum publicationDate where publicationDate <= requestedDate
        val validEntry = ratesMap.entries
            .filter { it.key <= requestedDate }
            .maxByOrNull { it.key }

        if (validEntry != null) {
            val diagnosticMsg = "RequestedDate=$requestedDate, Endpoint=$selectedEndpoint, HttpStatus=$httpStatus, Category=OFFICIAL, PubCount=${ratesMap.size}, EffectiveDate=${validEntry.key}"
            logDebugDiagnostic(diagnosticMsg)
            return@withContext BnrRateResult(
                requestedDate = requestedDate,
                effectiveDate = validEntry.key,
                rate = validEntry.value,
                source = "BNR_OFFICIAL",
                status = "OFFICIAL",
                diagnostic = diagnosticMsg
            )
        }

        // Try previous year archive if requested date is early January before first BNR publication in current year
        val prevYearUrl = "https://curs.bnr.ro/files/xml/years/nbrfxrates${year - 1}.xml"
        val (prevYearXml, prevStatus) = httpFetcher(prevYearUrl)
        if (prevYearXml != null) {
            val (prevMap, prevSuccess) = parseBnrXmlContentWithStatus(prevYearXml)
            if (prevSuccess) {
                val prevEntry = prevMap.entries
                    .filter { it.key <= requestedDate }
                    .maxByOrNull { it.key }

                if (prevEntry != null) {
                    val diagnosticMsg = "RequestedDate=$requestedDate, Endpoint=$prevYearUrl, HttpStatus=$prevStatus, Category=OFFICIAL, PubCount=${prevMap.size}, EffectiveDate=${prevEntry.key}"
                    logDebugDiagnostic(diagnosticMsg)
                    return@withContext BnrRateResult(
                        requestedDate = requestedDate,
                        effectiveDate = prevEntry.key,
                        rate = prevEntry.value,
                        source = "BNR_OFFICIAL",
                        status = "OFFICIAL",
                        diagnostic = diagnosticMsg
                    )
                }
            }
        }

        val diagnosticMsg = "RequestedDate=$requestedDate, Endpoint=$selectedEndpoint, HttpStatus=$httpStatus, Category=NO_APPLICABLE_DATE, PubCount=${ratesMap.size}"
        logDebugDiagnostic(diagnosticMsg)

        return@withContext BnrRateResult(
            requestedDate = requestedDate,
            effectiveDate = requestedDate,
            rate = 0.0,
            source = "NONE",
            status = "NO_APPLICABLE_DATE",
            diagnostic = diagnosticMsg
        )
    }

    /**
     * Debug-only diagnostic that performs one controlled read from https://curs.bnr.ro/nbrfxrates10days.xml.
     * Reports reachability, HTTP status, parsed publication count, whether EUR rate was found, and latest publication date.
     * Does NOT display or log financial transaction data.
     */
    suspend fun runDebugDiagnostic(): BnrDiagnosticResult = withContext(Dispatchers.IO) {
        val (content, status) = httpFetcher("https://curs.bnr.ro/nbrfxrates10days.xml")
        if (content == null) {
            return@withContext BnrDiagnosticResult(
                isReachable = false,
                httpStatus = status,
                publicationDatesParsed = 0,
                eurRateFound = false,
                latestPublicationDate = null
            )
        }

        val (ratesMap, xmlSuccess) = parseBnrXmlContentWithStatus(content)
        val isReachable = xmlSuccess && status == "200" && ratesMap.isNotEmpty()
        val latestDate = ratesMap.keys.maxOrNull()

        BnrDiagnosticResult(
            isReachable = isReachable,
            httpStatus = status,
            publicationDatesParsed = ratesMap.size,
            eurRateFound = ratesMap.isNotEmpty(),
            latestPublicationDate = latestDate
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

            // Secure DocumentBuilderFactory hardening against DTD and external entities
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

            var cubeList = doc.getElementsByTagNameNS("*", "Cube")
            if (cubeList.length == 0) {
                cubeList = doc.getElementsByTagName("Cube")
            }

            for (i in 0 until cubeList.length) {
                val cubeElement = cubeList.item(i) as? Element ?: continue
                val cubeDate = cubeElement.getAttribute("date")
                if (cubeDate.isNullOrBlank()) continue

                var rateList = cubeElement.getElementsByTagNameNS("*", "Rate")
                if (rateList.length == 0) {
                    rateList = cubeElement.getElementsByTagName("Rate")
                }

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

    private fun logDebugDiagnostic(message: String) {
        try {
            android.util.Log.d("FinTrackBNR", message)
        } catch (ignored: Throwable) {
            // Log class not present in unit test environment
        }
    }

    companion object {
        fun fetchUrlWithStatus(urlString: String): Pair<String?, String> {
            var conn: HttpURLConnection? = null
            return try {
                val url = URL(urlString)
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile) FinTrack/1.0")
                conn.setRequestProperty("Accept", "application/xml, text/xml, */*")

                val code = conn.responseCode
                if (code == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    if (text.isBlank()) {
                        Pair(null, "EMPTY_RESPONSE")
                    } else {
                        Pair(text, "200")
                    }
                } else {
                    Pair(null, code.toString())
                }
            } catch (e: SecurityException) {
                Pair(null, "NO_INTERNET_PERMISSION")
            } catch (e: java.net.UnknownHostException) {
                Pair(null, "DNS_FAILURE")
            } catch (e: javax.net.ssl.SSLException) {
                Pair(null, "TLS_FAILURE")
            } catch (e: java.net.SocketTimeoutException) {
                Pair(null, "TIMEOUT")
            } catch (e: java.net.ConnectException) {
                Pair(null, "TIMEOUT")
            } catch (e: java.io.IOException) {
                Pair(null, "NO_NETWORK")
            } catch (e: Exception) {
                Pair(null, "HTTP_ERROR")
            } finally {
                conn?.disconnect()
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

