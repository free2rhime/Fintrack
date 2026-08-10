package com.example.data.service

import com.example.data.dao.ExchangeRateDao
import com.example.data.model.ExchangeRateEntity
import java.io.InputStream
import java.io.StringReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

data class BnrRateResult(
    val requestedDate: String,
    val effectiveDate: String,
    val rate: Double,
    val source: String,
    val status: String,
    val diagnostic: String? = null
)

data class EndpointDiagnosticItem(
    val endpoint: String,
    val httpStatus: String,
    val contentType: String?,
    val byteCount: Int,
    val parseResult: String,
    val eurPublicationCount: Int
)

data class BnrDiagnosticResult(
    val isReachable: Boolean,
    val requestedUrl: String = "https://curs.bnr.ro/nbrfxrates10days.xml",
    val finalUrl: String = "https://curs.bnr.ro/nbrfxrates10days.xml",
    val httpStatus: String,
    val contentType: String? = null,
    val contentEncoding: String? = null,
    val responseByteCount: Int = 0,
    val hasXmlDeclaration: Boolean = false,
    val isHtml: Boolean = false,
    val rootQualifiedName: String? = null,
    val rootLocalName: String? = null,
    val rootNamespaceUri: String? = null,
    val cubeElementCount: Int = 0,
    val rateElementCount: Int = 0,
    val eurRateElementCount: Int = 0,
    val publicationDatesParsed: Int = 0,
    val eurRateFound: Boolean = false,
    val latestPublicationDate: String? = null,
    val failureCategory: String = "NONE",
    val stageA_httpConnection: Boolean = false,
    val stageB_bodyObtained: Boolean = false,
    val stageC_xmlOpened: Boolean = false,
    val stageD_cubeFound: Boolean = false,
    val stageE_rateFound: Boolean = false,
    val stageF_eurFound: Boolean = false,
    val stageG_validRatesProduced: Boolean = false,
    val sanitizedPreview: String? = null,
    val endpointDiagnostics: List<EndpointDiagnosticItem> = emptyList()
)

data class HttpResponseData(
    val requestedUrl: String,
    val finalUrl: String,
    val httpStatus: String,
    val contentType: String?,
    val contentEncoding: String?,
    val byteCount: Int,
    val content: String?,
    val isHtml: Boolean = false,
    val isGenericXml: Boolean = false
)

data class BnrXmlParseResult(
    val ratesMap: Map<String, Double> = emptyMap(),
    val failureCategory: String = "EMPTY_RESPONSE",
    val hasXmlDeclaration: Boolean = false,
    val rootQualifiedName: String? = null,
    val rootLocalName: String? = null,
    val rootNamespaceUri: String? = null,
    val cubeElementCount: Int = 0,
    val rateElementCount: Int = 0,
    val eurRateElementCount: Int = 0,
    val publicationDatesParsed: Int = 0,
    val latestPublicationDate: String? = null,
    val stageC_xmlOpened: Boolean = false,
    val stageD_cubeFound: Boolean = false,
    val stageE_rateFound: Boolean = false,
    val stageF_eurFound: Boolean = false,
    val stageG_validRatesProduced: Boolean = false,
    val exceptionClass: String? = null,
    val exceptionMessage: String? = null,
    val securityFeaturesApplied: Map<String, Boolean> = emptyMap()
)

class ExchangeRateService(
    private val exchangeRateDao: ExchangeRateDao,
    private val httpFetcher: (String) -> Pair<String?, String> = ExchangeRateService::fetchUrlWithStatus,
    private val detailedHttpFetcher: ((String) -> HttpResponseData)? = null
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

    private fun getDetailedResponse(url: String): HttpResponseData {
        if (detailedHttpFetcher != null) {
            return detailedHttpFetcher.invoke(url)
        }
        val isDefaultFetcher = (httpFetcher == ExchangeRateService::fetchUrlWithStatus)
        if (isDefaultFetcher) {
            return fetchUrlWithResponseData(url)
        }
        val (content, status) = httpFetcher(url)
        val trimmed = content?.trim()
        val isHtml = trimmed?.let { it.lowercase().startsWith("<!doctype html") || it.lowercase().startsWith("<html") } == true
        val isXmlType = !isHtml && (trimmed?.startsWith("<?xml", ignoreCase = true) == true || trimmed?.contains("<DataSet", ignoreCase = true) == true)
        return HttpResponseData(
            requestedUrl = url,
            finalUrl = url,
            httpStatus = status,
            contentType = if (isHtml) "text/html" else if (isXmlType) "application/xml" else null,
            contentEncoding = null,
            byteCount = content?.toByteArray(Charsets.UTF_8)?.size ?: 0,
            content = content,
            isHtml = isHtml,
            isGenericXml = !isHtml && !isXmlType && trimmed != null
        )
    }

    /**
     * Fetches official BNR XML endpoints in fallback sequence and extracts EUR rate.
     * Records individual endpoint diagnostic details.
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
        val candidateEndpoints = listOf(
            "https://curs.bnr.ro/files/xml/years/nbrfxrates$year.xml",
            "https://curs.bnr.ro/nbrfxrates10days.xml",
            "https://curs.bnr.ro/nbrfxrates.xml"
        )

        val endpointDiagnostics = mutableListOf<EndpointDiagnosticItem>()
        var primaryFailureCategory = "HTTP_ERROR"

        for (endpoint in candidateEndpoints) {
            val response = getDetailedResponse(endpoint)
            if (response.httpStatus != "200" || response.content.isNullOrBlank()) {
                val cat = if (response.httpStatus in listOf("NO_INTERNET_PERMISSION", "NO_NETWORK", "DNS_FAILURE", "TLS_FAILURE", "TIMEOUT", "EMPTY_RESPONSE")) response.httpStatus else "HTTP_ERROR"
                primaryFailureCategory = cat
                endpointDiagnostics.add(
                    EndpointDiagnosticItem(
                        endpoint = endpoint,
                        httpStatus = response.httpStatus,
                        contentType = response.contentType,
                        byteCount = response.byteCount,
                        parseResult = cat,
                        eurPublicationCount = 0
                    )
                )
                continue
            }

            if (response.isHtml) {
                primaryFailureCategory = "RESPONSE_IS_HTML"
                endpointDiagnostics.add(
                    EndpointDiagnosticItem(
                        endpoint = endpoint,
                        httpStatus = "200",
                        contentType = response.contentType,
                        byteCount = response.byteCount,
                        parseResult = "RESPONSE_IS_HTML",
                        eurPublicationCount = 0
                    )
                )
                continue
            }

            val parseResult = parseBnrXmlDetailed(response.content)
            endpointDiagnostics.add(
                EndpointDiagnosticItem(
                    endpoint = endpoint,
                    httpStatus = "200",
                    contentType = response.contentType,
                    byteCount = response.byteCount,
                    parseResult = parseResult.failureCategory,
                    eurPublicationCount = parseResult.publicationDatesParsed
                )
            )

            if (parseResult.failureCategory == "OFFICIAL_RATES_PARSED" && parseResult.ratesMap.isNotEmpty()) {
                val validEntry = parseResult.ratesMap.entries
                    .filter { it.key <= requestedDate }
                    .maxByOrNull { it.key }

                if (validEntry != null) {
                    val diagMsg = "RequestedDate=$requestedDate, Endpoint=$endpoint, EffectiveDate=${validEntry.key}, Rate=${validEntry.value}, PubCount=${parseResult.ratesMap.size}, Attempts=${endpointDiagnostics.size}"
                    logDebugDiagnostic(diagMsg)
                    return@withContext BnrRateResult(
                        requestedDate = requestedDate,
                        effectiveDate = validEntry.key,
                        rate = validEntry.value,
                        source = "BNR_OFFICIAL",
                        status = "OFFICIAL",
                        diagnostic = diagMsg
                    )
                }
            } else {
                primaryFailureCategory = parseResult.failureCategory
            }
        }

        // Try previous year archive if early January before first BNR publication in current year
        if (parsedDate.monthValue == 1) {
            val prevYearEndpoint = "https://curs.bnr.ro/files/xml/years/nbrfxrates${year - 1}.xml"
            val prevResponse = getDetailedResponse(prevYearEndpoint)
            if (prevResponse.httpStatus == "200" && !prevResponse.content.isNullOrBlank() && !prevResponse.isHtml) {
                val parseResult = parseBnrXmlDetailed(prevResponse.content)
                endpointDiagnostics.add(
                    EndpointDiagnosticItem(
                        endpoint = prevYearEndpoint,
                        httpStatus = "200",
                        contentType = prevResponse.contentType,
                        byteCount = prevResponse.byteCount,
                        parseResult = parseResult.failureCategory,
                        eurPublicationCount = parseResult.publicationDatesParsed
                    )
                )

                if (parseResult.failureCategory == "OFFICIAL_RATES_PARSED" && parseResult.ratesMap.isNotEmpty()) {
                    val prevEntry = parseResult.ratesMap.entries
                        .filter { it.key <= requestedDate }
                        .maxByOrNull { it.key }

                    if (prevEntry != null) {
                        val diagMsg = "RequestedDate=$requestedDate, Endpoint=$prevYearEndpoint, EffectiveDate=${prevEntry.key}, Rate=${prevEntry.value}, PubCount=${parseResult.ratesMap.size}"
                        logDebugDiagnostic(diagMsg)
                        return@withContext BnrRateResult(
                            requestedDate = requestedDate,
                            effectiveDate = prevEntry.key,
                            rate = prevEntry.value,
                            source = "BNR_OFFICIAL",
                            status = "OFFICIAL",
                            diagnostic = diagMsg
                        )
                    }
                }
            }
        }

        val diagMsg = "RequestedDate=$requestedDate, Status=$primaryFailureCategory, Diagnostics=[${endpointDiagnostics.joinToString { "${it.endpoint}=${it.parseResult}" }}]"
        logDebugDiagnostic(diagMsg)

        return@withContext BnrRateResult(
            requestedDate = requestedDate,
            effectiveDate = requestedDate,
            rate = 0.0,
            source = "NONE",
            status = primaryFailureCategory,
            diagnostic = diagMsg
        )
    }

    /**
     * Debug-only diagnostic that performs a controlled inspection of BNR endpoints.
     */
    suspend fun runDebugDiagnostic(): BnrDiagnosticResult = withContext(Dispatchers.IO) {
        val targetUrl = "https://curs.bnr.ro/nbrfxrates10days.xml"
        val response = getDetailedResponse(targetUrl)

        if (response.content.isNullOrBlank() || response.httpStatus != "200") {
            return@withContext BnrDiagnosticResult(
                isReachable = false,
                requestedUrl = response.requestedUrl,
                finalUrl = response.finalUrl,
                httpStatus = response.httpStatus,
                contentType = response.contentType,
                contentEncoding = response.contentEncoding,
                responseByteCount = response.byteCount,
                hasXmlDeclaration = false,
                isHtml = response.isHtml,
                publicationDatesParsed = 0,
                eurRateFound = false,
                latestPublicationDate = null,
                failureCategory = if (response.httpStatus == "200") "EMPTY_RESPONSE" else response.httpStatus,
                stageA_httpConnection = response.httpStatus == "200",
                stageB_bodyObtained = false
            )
        }

        val parseResult = parseBnrXmlDetailed(response.content)
        val isReachable = (response.httpStatus == "200") &&
                (parseResult.failureCategory == "OFFICIAL_RATES_PARSED") &&
                parseResult.ratesMap.isNotEmpty()

        val preview = response.content.replace(Regex("[\\r\\n]+"), " ").trim().take(200)

        BnrDiagnosticResult(
            isReachable = isReachable,
            requestedUrl = response.requestedUrl,
            finalUrl = response.finalUrl,
            httpStatus = response.httpStatus,
            contentType = response.contentType,
            contentEncoding = response.contentEncoding,
            responseByteCount = response.byteCount,
            hasXmlDeclaration = parseResult.hasXmlDeclaration,
            isHtml = response.isHtml,
            rootQualifiedName = parseResult.rootQualifiedName,
            rootLocalName = parseResult.rootLocalName,
            rootNamespaceUri = parseResult.rootNamespaceUri,
            cubeElementCount = parseResult.cubeElementCount,
            rateElementCount = parseResult.rateElementCount,
            eurRateElementCount = parseResult.eurRateElementCount,
            publicationDatesParsed = parseResult.publicationDatesParsed,
            eurRateFound = parseResult.eurRateElementCount > 0 && parseResult.ratesMap.isNotEmpty(),
            latestPublicationDate = parseResult.latestPublicationDate,
            failureCategory = parseResult.failureCategory,
            stageA_httpConnection = response.httpStatus == "200",
            stageB_bodyObtained = response.byteCount > 0,
            stageC_xmlOpened = parseResult.stageC_xmlOpened,
            stageD_cubeFound = parseResult.stageD_cubeFound,
            stageE_rateFound = parseResult.stageE_rateFound,
            stageF_eurFound = parseResult.stageF_eurFound,
            stageG_validRatesProduced = parseResult.stageG_validRatesProduced,
            sanitizedPreview = preview
        )
    }

    fun parseBnrXmlStream(inputStream: InputStream): Map<String, Double> {
        return parseBnrXmlStreamWithStatus(inputStream).first
    }

    fun parseBnrXmlStreamWithStatus(inputStream: InputStream): Pair<Map<String, Double>, Boolean> {
        val text = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val res = parseBnrXmlDetailed(text)
        return Pair(res.ratesMap, res.failureCategory == "OFFICIAL_RATES_PARSED")
    }

    fun parseBnrXmlContent(xml: String): Map<String, Double> {
        return parseBnrXmlDetailed(xml).ratesMap
    }

    fun parseBnrXmlContentWithStatus(xml: String): Pair<Map<String, Double>, Boolean> {
        val res = parseBnrXmlDetailed(xml)
        return Pair(res.ratesMap, res.failureCategory == "OFFICIAL_RATES_PARSED")
    }

    fun parseBnrXmlDetailed(xml: String): BnrXmlParseResult {
        if (xml.isBlank()) {
            return BnrXmlParseResult(failureCategory = "EMPTY_RESPONSE")
        }

        val trimmed = xml.trim()
        val hasXmlDeclaration = trimmed.startsWith("<?xml", ignoreCase = true)
        val isHtml = trimmed.lowercase().startsWith("<!doctype html") || trimmed.lowercase().startsWith("<html")

        if (isHtml) {
            return BnrXmlParseResult(
                failureCategory = "RESPONSE_IS_HTML",
                hasXmlDeclaration = false,
                stageC_xmlOpened = false
            )
        }

        val securityApplied = mutableMapOf<String, Boolean>()
        val ratesMap = mutableMapOf<String, Double>()

        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.isXIncludeAware = false
        factory.isExpandEntityReferences = false

        fun setFeatureSafe(feature: String, value: Boolean) {
            try {
                factory.setFeature(feature, value)
                securityApplied[feature] = value
            } catch (e: Exception) {
                securityApplied[feature] = false
            }
        }

        setFeatureSafe(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        setFeatureSafe("http://xml.org/sax/features/external-general-entities", false)
        setFeatureSafe("http://xml.org/sax/features/external-parameter-entities", false)
        setFeatureSafe("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)

        val doc: Document = try {
            val builder = factory.newDocumentBuilder()
            builder.setEntityResolver { _, _ -> InputSource(StringReader("")) }
            builder.parse(xml.byteInputStream(Charsets.UTF_8))
        } catch (e: Exception) {
            return BnrXmlParseResult(
                failureCategory = "XML_DOCUMENT_INVALID",
                hasXmlDeclaration = hasXmlDeclaration,
                exceptionClass = e.javaClass.simpleName,
                exceptionMessage = e.message?.take(150),
                securityFeaturesApplied = securityApplied
            )
        }

        val stageC = true
        val root = doc.documentElement
        val rootQualifiedName = root?.nodeName
        val rootLocalName = root?.localName ?: root?.nodeName?.substringAfterLast(':')
        val rootNamespaceUri = root?.namespaceURI

        if (rootLocalName.equals("DataSet", ignoreCase = true) != true) {
            return BnrXmlParseResult(
                failureCategory = "UNEXPECTED_ROOT",
                hasXmlDeclaration = hasXmlDeclaration,
                rootQualifiedName = rootQualifiedName,
                rootLocalName = rootLocalName,
                rootNamespaceUri = rootNamespaceUri,
                stageC_xmlOpened = stageC,
                securityFeaturesApplied = securityApplied
            )
        }

        val allCubes = mutableListOf<Element>()
        findElementsRecursively(doc, "Cube", allCubes)
        val cubeElementCount = allCubes.size

        if (cubeElementCount == 0) {
            return BnrXmlParseResult(
                failureCategory = "CUBE_NOT_FOUND",
                hasXmlDeclaration = hasXmlDeclaration,
                rootQualifiedName = rootQualifiedName,
                rootLocalName = rootLocalName,
                rootNamespaceUri = rootNamespaceUri,
                cubeElementCount = 0,
                stageC_xmlOpened = stageC,
                securityFeaturesApplied = securityApplied
            )
        }

        var totalRateCount = 0
        var totalEurRateCount = 0
        var datedCubeCount = 0

        for (cubeElement in allCubes) {
            val dateAttr = cubeElement.getAttribute("date").takeIf { it.isNotBlank() }
                ?: cubeElement.getAttribute("Date").takeIf { it.isNotBlank() }
                ?: continue

            val validDate = try {
                LocalDate.parse(dateAttr, DateTimeFormatter.ISO_LOCAL_DATE).toString()
            } catch (e: Exception) {
                null
            } ?: continue

            datedCubeCount++

            val ratesInCube = mutableListOf<Element>()
            findElementsRecursively(cubeElement, "Rate", ratesInCube)
            totalRateCount += ratesInCube.size

            for (rateElement in ratesInCube) {
                val curr = rateElement.getAttribute("currency").takeIf { it.isNotBlank() }
                    ?: rateElement.getAttribute("Currency").takeIf { it.isNotBlank() }
                    ?: continue

                if (curr.trim().equals("EUR", ignoreCase = true)) {
                    totalEurRateCount++
                    val rawText = rateElement.textContent?.trim() ?: continue
                    val parsedRate = rawText.toDoubleOrNull()
                    if (parsedRate != null && parsedRate > 0.0 && !parsedRate.isNaN() && !parsedRate.isInfinite()) {
                        val multAttr = rateElement.getAttribute("multiplier").takeIf { it.isNotBlank() }
                            ?: rateElement.getAttribute("Multiplier").takeIf { it.isNotBlank() }
                        val multVal = multAttr?.toDoubleOrNull() ?: 1.0
                        val finalMultiplier = if (multVal > 0.0) multVal else 1.0
                        ratesMap[validDate] = parsedRate / finalMultiplier
                    }
                }
            }
        }

        val stageD = cubeElementCount > 0
        val stageE = totalRateCount > 0
        val stageF = totalEurRateCount > 0
        val stageG = ratesMap.isNotEmpty()

        val latestDate = ratesMap.keys.maxOrNull()

        val failureCategory = when {
            datedCubeCount == 0 -> "DATED_CUBE_NOT_FOUND"
            totalRateCount == 0 -> "RATE_NOT_FOUND"
            totalEurRateCount == 0 -> "EUR_RATE_NOT_FOUND"
            ratesMap.isEmpty() -> "INVALID_EUR_VALUE"
            else -> "OFFICIAL_RATES_PARSED"
        }

        return BnrXmlParseResult(
            ratesMap = ratesMap,
            failureCategory = failureCategory,
            hasXmlDeclaration = hasXmlDeclaration,
            rootQualifiedName = rootQualifiedName,
            rootLocalName = rootLocalName,
            rootNamespaceUri = rootNamespaceUri,
            cubeElementCount = cubeElementCount,
            rateElementCount = totalRateCount,
            eurRateElementCount = totalEurRateCount,
            publicationDatesParsed = ratesMap.size,
            latestPublicationDate = latestDate,
            stageC_xmlOpened = stageC,
            stageD_cubeFound = stageD,
            stageE_rateFound = stageE,
            stageF_eurFound = stageF,
            stageG_validRatesProduced = stageG,
            securityFeaturesApplied = securityApplied
        )
    }

    private fun findElementsRecursively(node: Node, targetLocalName: String, result: MutableList<Element>) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            val elem = node as Element
            val local = elem.localName ?: elem.nodeName.substringAfterLast(':')
            if (local.equals(targetLocalName, ignoreCase = true)) {
                result.add(elem)
            }
        }
        val children = node.childNodes
        for (i in 0 until children.length) {
            findElementsRecursively(children.item(i), targetLocalName, result)
        }
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
            val res = fetchUrlWithResponseData(urlString)
            if (res.httpStatus != "200") {
                return Pair(null, res.httpStatus)
            }
            if (res.content.isNullOrBlank()) {
                return Pair(null, "EMPTY_RESPONSE")
            }
            if (res.isHtml) {
                return Pair(res.content, "200")
            }
            return Pair(res.content, "200")
        }

        fun fetchUrlWithResponseData(urlString: String): HttpResponseData {
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
                conn.setRequestProperty("Accept-Encoding", "gzip, deflate")

                val code = conn.responseCode
                val finalUrl = conn.url?.toString() ?: urlString
                val contentType = conn.contentType
                val contentEncoding = conn.contentEncoding

                if (code == 200) {
                    val rawStream = conn.inputStream
                    val decompressedStream = when {
                        contentEncoding?.lowercase()?.contains("gzip") == true -> GZIPInputStream(rawStream)
                        contentEncoding?.lowercase()?.contains("deflate") == true -> InflaterInputStream(rawStream)
                        else -> rawStream
                    }

                    val bytes = decompressedStream.use { it.readBytes() }
                    if (bytes.isEmpty()) {
                        HttpResponseData(
                            requestedUrl = urlString,
                            finalUrl = finalUrl,
                            httpStatus = "200",
                            contentType = contentType,
                            contentEncoding = contentEncoding,
                            byteCount = 0,
                            content = null
                        )
                    } else {
                        val charsetName = extractCharsetFromContentType(contentType)
                        val charset = try {
                            if (charsetName != null) Charset.forName(charsetName) else Charsets.UTF_8
                        } catch (_: Exception) {
                            Charsets.UTF_8
                        }

                        val text = String(bytes, charset)
                        val trimmed = text.trim()

                        val isHtml = contentType?.lowercase()?.contains("text/html") == true ||
                                contentType?.lowercase()?.contains("application/xhtml+xml") == true ||
                                trimmed.lowercase().startsWith("<!doctype html") ||
                                trimmed.lowercase().startsWith("<html")

                        val isXmlType = contentType?.lowercase()?.let {
                            it.contains("application/xml") || it.contains("text/xml") || it.contains("+xml")
                        } ?: false

                        val isGenericXml = !isXmlType && !isHtml && (trimmed.startsWith("<?xml", ignoreCase = true) || trimmed.contains("<DataSet", ignoreCase = true))

                        HttpResponseData(
                            requestedUrl = urlString,
                            finalUrl = finalUrl,
                            httpStatus = "200",
                            contentType = contentType,
                            contentEncoding = contentEncoding,
                            byteCount = bytes.size,
                            content = text,
                            isHtml = isHtml,
                            isGenericXml = isGenericXml
                        )
                    }
                } else {
                    HttpResponseData(
                        requestedUrl = urlString,
                        finalUrl = finalUrl,
                        httpStatus = code.toString(),
                        contentType = contentType,
                        contentEncoding = contentEncoding,
                        byteCount = 0,
                        content = null
                    )
                }
            } catch (e: SecurityException) {
                HttpResponseData(urlString, urlString, "NO_INTERNET_PERMISSION", null, null, 0, null)
            } catch (e: java.net.UnknownHostException) {
                HttpResponseData(urlString, urlString, "DNS_FAILURE", null, null, 0, null)
            } catch (e: javax.net.ssl.SSLException) {
                HttpResponseData(urlString, urlString, "TLS_FAILURE", null, null, 0, null)
            } catch (e: java.net.SocketTimeoutException) {
                HttpResponseData(urlString, urlString, "TIMEOUT", null, null, 0, null)
            } catch (e: java.net.ConnectException) {
                HttpResponseData(urlString, urlString, "TIMEOUT", null, null, 0, null)
            } catch (e: java.io.IOException) {
                HttpResponseData(urlString, urlString, "NO_NETWORK", null, null, 0, null)
            } catch (e: Exception) {
                HttpResponseData(urlString, urlString, "HTTP_ERROR", null, null, 0, null)
            } finally {
                conn?.disconnect()
            }
        }

        private fun extractCharsetFromContentType(contentType: String?): String? {
            if (contentType == null) return null
            val parts = contentType.split(";")
            for (part in parts) {
                val trimmed = part.trim()
                if (trimmed.lowercase().startsWith("charset=")) {
                    return trimmed.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                }
            }
            return null
        }

        fun calculateAmountEUR(amountRON: Double, rate: Double): Double {
            if (rate <= 0.0) return 0.0
            return BigDecimal.valueOf(amountRON)
                .divide(BigDecimal.valueOf(rate), 2, RoundingMode.HALF_UP)
                .toDouble()
        }
    }
}
