package com.example.data.model

import com.google.firebase.firestore.PropertyName

data class ExchangeRateMetadataDto(
    @get:PropertyName("source") @set:PropertyName("source") var source: String? = null,
    @get:PropertyName("status") @set:PropertyName("status") var status: String? = null,
    @get:PropertyName("rate") @set:PropertyName("rate") var rate: Double? = null,
    @get:PropertyName("effectiveDate") @set:PropertyName("effectiveDate") var effectiveDate: String? = null
)

data class TransactionDto(
    @get:PropertyName("transactionId") @set:PropertyName("transactionId") var transactionId: String? = null,
    @get:PropertyName("householdId") @set:PropertyName("householdId") var householdId: String? = null,
    @get:PropertyName("createdByUid") @set:PropertyName("createdByUid") var createdByUid: String? = null,
    @get:PropertyName("transactionDate") @set:PropertyName("transactionDate") var transactionDate: String? = null,
    @get:PropertyName("description") @set:PropertyName("description") var description: String? = null,
    @get:PropertyName("amountRon") @set:PropertyName("amountRon") var amountRon: Double? = null,
    @get:PropertyName("amountEur") @set:PropertyName("amountEur") var amountEur: Double? = null,
    @get:PropertyName("exchangeRate") @set:PropertyName("exchangeRate") var exchangeRate: Double? = null,
    @get:PropertyName("exchangeRateDate") @set:PropertyName("exchangeRateDate") var exchangeRateDate: String? = null,
    @get:PropertyName("type") @set:PropertyName("type") var type: String? = null,
    @get:PropertyName("account") @set:PropertyName("account") var account: String? = null,
    @get:PropertyName("category") @set:PropertyName("category") var category: String? = null,
    @get:PropertyName("subCategory") @set:PropertyName("subCategory") var subCategory: String? = "",
    @get:PropertyName("destination") @set:PropertyName("destination") var destination: String? = null,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long? = null,
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt") var updatedAt: Long? = null,
    @get:PropertyName("exchangeRateMetadata") @set:PropertyName("exchangeRateMetadata") var exchangeRateMetadata: ExchangeRateMetadataDto? = null,
    @get:PropertyName("exchangeRateSource") @set:PropertyName("exchangeRateSource") var exchangeRateSource: String? = null,
    @get:PropertyName("conversionStatus") @set:PropertyName("conversionStatus") var conversionStatus: String? = null,
    @get:PropertyName("categoryId") @set:PropertyName("categoryId") var categoryId: String? = null,
    @get:PropertyName("subCategoryId") @set:PropertyName("subCategoryId") var subCategoryId: String? = null,
    @get:PropertyName("isDeleted") @set:PropertyName("isDeleted") var isDeleted: Boolean? = false
) {
    companion object {
        fun fromMap(map: Map<String, Any?>, docId: String): TransactionDto {
            val metaMap = map["exchangeRateMetadata"] as? Map<*, *>
            val metaDto = if (metaMap != null) {
                ExchangeRateMetadataDto(
                    source = metaMap["source"] as? String,
                    status = metaMap["status"] as? String,
                    rate = (metaMap["rate"] as? Number)?.toDouble(),
                    effectiveDate = metaMap["effectiveDate"] as? String
                )
            } else null

            return TransactionDto(
                transactionId = (map["transactionId"] as? String) ?: docId,
                householdId = map["householdId"] as? String,
                createdByUid = map["createdByUid"] as? String,
                transactionDate = map["transactionDate"] as? String,
                description = map["description"] as? String,
                amountRon = (map["amountRon"] as? Number)?.toDouble(),
                amountEur = (map["amountEur"] as? Number)?.toDouble(),
                exchangeRate = (map["exchangeRate"] as? Number)?.toDouble(),
                exchangeRateDate = map["exchangeRateDate"] as? String,
                type = map["type"] as? String,
                account = map["account"] as? String,
                category = map["category"] as? String,
                subCategory = (map["subCategory"] as? String) ?: "",
                destination = map["destination"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong(),
                exchangeRateMetadata = metaDto,
                exchangeRateSource = map["exchangeRateSource"] as? String,
                conversionStatus = map["conversionStatus"] as? String,
                categoryId = map["categoryId"] as? String,
                subCategoryId = map["subCategoryId"] as? String,
                isDeleted = map["isDeleted"] as? Boolean ?: false
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        val metaMap = exchangeRateMetadata?.let {
            mapOf(
                "source" to it.source,
                "status" to it.status,
                "rate" to it.rate,
                "effectiveDate" to it.effectiveDate
            )
        } ?: mapOf(
            "source" to exchangeRateSource,
            "status" to conversionStatus,
            "rate" to exchangeRate,
            "effectiveDate" to exchangeRateDate
        )

        return mapOf(
            "transactionId" to transactionId,
            "householdId" to householdId,
            "createdByUid" to createdByUid,
            "transactionDate" to transactionDate,
            "description" to description,
            "amountRon" to amountRon,
            "amountEur" to amountEur,
            "exchangeRate" to exchangeRate,
            "exchangeRateDate" to exchangeRateDate,
            "type" to type,
            "account" to account,
            "category" to category,
            "subCategory" to subCategory,
            "destination" to destination,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "exchangeRateMetadata" to metaMap,
            "exchangeRateSource" to exchangeRateSource,
            "conversionStatus" to conversionStatus,
            "categoryId" to categoryId,
            "subCategoryId" to subCategoryId,
            "isDeleted" to (isDeleted ?: false)
        )
    }
}

data class CategoryDto(
    @get:PropertyName("categoryId") @set:PropertyName("categoryId") var categoryId: String? = null,
    @get:PropertyName("householdId") @set:PropertyName("householdId") var householdId: String? = null,
    @get:PropertyName("name") @set:PropertyName("name") var name: String? = null,
    @get:PropertyName("type") @set:PropertyName("type") var type: String? = null,
    @get:PropertyName("subCategory") @set:PropertyName("subCategory") var subCategory: String? = "",
    @get:PropertyName("createdByUid") @set:PropertyName("createdByUid") var createdByUid: String? = null,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long? = null,
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt") var updatedAt: Long? = null,
    @get:PropertyName("isDeleted") @set:PropertyName("isDeleted") var isDeleted: Boolean? = false
) {
    companion object {
        fun fromMap(map: Map<String, Any?>, docId: String): CategoryDto {
            return CategoryDto(
                categoryId = (map["categoryId"] as? String) ?: docId,
                householdId = map["householdId"] as? String,
                name = map["name"] as? String,
                type = map["type"] as? String,
                subCategory = (map["subCategory"] as? String) ?: "",
                createdByUid = map["createdByUid"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong(),
                isDeleted = map["isDeleted"] as? Boolean ?: false
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "categoryId" to categoryId,
            "householdId" to householdId,
            "name" to name,
            "type" to type,
            "subCategory" to subCategory,
            "createdByUid" to createdByUid,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "isDeleted" to (isDeleted ?: false)
        )
    }
}

data class ExchangeRateDto(
    @get:PropertyName("requestedDate") @set:PropertyName("requestedDate") var requestedDate: String? = null,
    @get:PropertyName("effectiveDate") @set:PropertyName("effectiveDate") var effectiveDate: String? = null,
    @get:PropertyName("rate") @set:PropertyName("rate") var rate: Double? = null,
    @get:PropertyName("source") @set:PropertyName("source") var source: String? = null,
    @get:PropertyName("fetchedAt") @set:PropertyName("fetchedAt") var fetchedAt: Long? = null,
    @get:PropertyName("status") @set:PropertyName("status") var status: String? = null,
    @get:PropertyName("householdId") @set:PropertyName("householdId") var householdId: String? = null,
    @get:PropertyName("migrationId") @set:PropertyName("migrationId") var migrationId: String? = null,
    @get:PropertyName("rates") @set:PropertyName("rates") var rates: Map<String, Double>? = null
) {
    companion object {
        fun fromEntity(entity: ExchangeRateEntity, householdId: String, migrationId: String? = null): ExchangeRateDto {
            return ExchangeRateDto(
                requestedDate = entity.requestedDate.ifBlank { entity.date },
                effectiveDate = entity.effectiveDate.ifBlank { entity.date },
                rate = entity.rate,
                source = entity.source,
                fetchedAt = entity.fetchedAt,
                status = entity.status,
                householdId = householdId,
                migrationId = migrationId,
                rates = mapOf("EUR" to entity.rate)
            )
        }

        fun fromMap(map: Map<String, Any?>, docId: String): ExchangeRateDto {
            val ratesMap = (map["rates"] as? Map<*, *>)?.entries?.associate {
                (it.key as String) to ((it.value as? Number)?.toDouble() ?: 0.0)
            }
            return ExchangeRateDto(
                requestedDate = (map["requestedDate"] as? String) ?: docId,
                effectiveDate = map["effectiveDate"] as? String,
                rate = (map["rate"] as? Number)?.toDouble(),
                source = map["source"] as? String,
                fetchedAt = (map["fetchedAt"] as? Number)?.toLong(),
                status = map["status"] as? String,
                householdId = map["householdId"] as? String,
                migrationId = map["migrationId"] as? String,
                rates = ratesMap
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "requestedDate" to requestedDate,
            "effectiveDate" to effectiveDate,
            "rate" to rate,
            "source" to source,
            "fetchedAt" to fetchedAt,
            "status" to status,
            "householdId" to householdId,
            "migrationId" to migrationId,
            "rates" to (rates ?: mapOf("EUR" to (rate ?: 0.0)))
        )
    }
}

data class MigrationStateDto(
    @get:PropertyName("migrationId") @set:PropertyName("migrationId") var migrationId: String? = null,
    @get:PropertyName("householdId") @set:PropertyName("householdId") var householdId: String? = null,
    @get:PropertyName("initiatedByUid") @set:PropertyName("initiatedByUid") var initiatedByUid: String? = null,
    @get:PropertyName("stage") @set:PropertyName("stage") var stage: String? = null,
    @get:PropertyName("processedCount") @set:PropertyName("processedCount") var processedCount: Int? = null,
    @get:PropertyName("totalCount") @set:PropertyName("totalCount") var totalCount: Int? = null,
    @get:PropertyName("currentPhase") @set:PropertyName("currentPhase") var currentPhase: String? = null,
    @get:PropertyName("lastProcessedId") @set:PropertyName("lastProcessedId") var lastProcessedId: String? = null,
    @get:PropertyName("lastError") @set:PropertyName("lastError") var lastError: String? = null,
    @get:PropertyName("backupPath") @set:PropertyName("backupPath") var backupPath: String? = null,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long? = null,
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt") var updatedAt: Long? = null
) {
    companion object {
        fun fromEntity(entity: MigrationStateEntity): MigrationStateDto {
            return MigrationStateDto(
                migrationId = entity.migrationId,
                householdId = entity.householdId,
                initiatedByUid = entity.initiatedByUid,
                stage = entity.stage,
                processedCount = entity.processedCount,
                totalCount = entity.totalCount,
                currentPhase = entity.currentPhase,
                lastProcessedId = entity.lastProcessedId,
                lastError = entity.lastError,
                backupPath = entity.backupPath,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

data class HouseholdDto(
    @get:PropertyName("householdId") @set:PropertyName("householdId") var householdId: String? = null,
    @get:PropertyName("name") @set:PropertyName("name") var name: String? = null,
    @get:PropertyName("createdByUid") @set:PropertyName("createdByUid") var createdByUid: String? = null,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long? = null,
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt") var updatedAt: Long? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>, docId: String): HouseholdDto {
            return HouseholdDto(
                householdId = (map["householdId"] as? String) ?: docId,
                name = map["name"] as? String,
                createdByUid = map["createdByUid"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong()
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "householdId" to householdId,
            "name" to name,
            "createdByUid" to createdByUid,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
}

data class HouseholdMemberDto(
    @get:PropertyName("uid") @set:PropertyName("uid") var uid: String? = null,
    @get:PropertyName("email") @set:PropertyName("email") var email: String? = null,
    @get:PropertyName("displayName") @set:PropertyName("displayName") var displayName: String? = null,
    @get:PropertyName("role") @set:PropertyName("role") var role: String? = null,
    @get:PropertyName("status") @set:PropertyName("status") var status: String? = null,
    @get:PropertyName("joinedAt") @set:PropertyName("joinedAt") var joinedAt: Long? = null,
    @get:PropertyName("invitedByUid") @set:PropertyName("invitedByUid") var invitedByUid: String? = null,
    @get:PropertyName("inviteId") @set:PropertyName("inviteId") var inviteId: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>, docId: String): HouseholdMemberDto {
            return HouseholdMemberDto(
                uid = (map["uid"] as? String) ?: docId,
                email = map["email"] as? String,
                displayName = map["displayName"] as? String,
                role = map["role"] as? String,
                status = map["status"] as? String,
                joinedAt = (map["joinedAt"] as? Number)?.toLong(),
                invitedByUid = map["invitedByUid"] as? String,
                inviteId = map["inviteId"] as? String
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "uid" to uid,
            "email" to email,
            "displayName" to displayName,
            "role" to role,
            "status" to status,
            "joinedAt" to joinedAt,
            "invitedByUid" to invitedByUid
        )
        if (inviteId != null) {
            map["inviteId"] = inviteId
        }
        return map
    }
}

data class HouseholdInviteDto(
    @get:PropertyName("inviteId") @set:PropertyName("inviteId") var inviteId: String? = null,
    @get:PropertyName("householdId") @set:PropertyName("householdId") var householdId: String? = null,
    @get:PropertyName("householdName") @set:PropertyName("householdName") var householdName: String? = null,
    @get:PropertyName("inviterUid") @set:PropertyName("inviterUid") var inviterUid: String? = null,
    @get:PropertyName("inviterEmail") @set:PropertyName("inviterEmail") var inviterEmail: String? = null,
    @get:PropertyName("inviterDisplayName") @set:PropertyName("inviterDisplayName") var inviterDisplayName: String? = null,
    @get:PropertyName("inviteeEmail") @set:PropertyName("inviteeEmail") var inviteeEmail: String? = null,
    @get:PropertyName("targetRole") @set:PropertyName("targetRole") var targetRole: String? = "member",
    @get:PropertyName("status") @set:PropertyName("status") var status: String? = "PENDING",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long? = null,
    @get:PropertyName("expiresAt") @set:PropertyName("expiresAt") var expiresAt: Long? = null,
    @get:PropertyName("respondedAt") @set:PropertyName("respondedAt") var respondedAt: Long? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>, docId: String): HouseholdInviteDto {
            return HouseholdInviteDto(
                inviteId = (map["inviteId"] as? String) ?: docId,
                householdId = map["householdId"] as? String,
                householdName = map["householdName"] as? String,
                inviterUid = map["inviterUid"] as? String,
                inviterEmail = map["inviterEmail"] as? String,
                inviterDisplayName = map["inviterDisplayName"] as? String,
                inviteeEmail = map["inviteeEmail"] as? String,
                targetRole = (map["targetRole"] as? String) ?: "member",
                status = (map["status"] as? String) ?: "PENDING",
                createdAt = (map["createdAt"] as? Number)?.toLong(),
                expiresAt = (map["expiresAt"] as? Number)?.toLong(),
                respondedAt = (map["respondedAt"] as? Number)?.toLong()
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "inviteId" to inviteId,
            "householdId" to householdId,
            "householdName" to householdName,
            "inviterUid" to inviterUid,
            "inviterEmail" to inviterEmail,
            "inviterDisplayName" to inviterDisplayName,
            "inviteeEmail" to inviteeEmail,
            "targetRole" to targetRole,
            "status" to status,
            "createdAt" to createdAt,
            "expiresAt" to expiresAt,
            "respondedAt" to respondedAt
        )
    }
}

object FirestoreDtoValidator {

    fun isValidTransactionType(type: String?): Boolean {
        return type != null && type in listOf("Income", "Expense")
    }

    fun isValidAccount(account: String?): Boolean {
        return account != null && account in listOf("Card", "Cash", "Meal Tickets")
    }

    fun isValidDestination(type: String?, destination: String?): Boolean {
        return when (type) {
            "Income" -> destination == null || destination in listOf("Bubu", "Piticania")
            "Expense" -> destination == null
            else -> false
        }
    }

    fun isValidConversionMetadata(meta: ExchangeRateMetadataDto?, transactionDate: String): Boolean {
        if (meta == null) return false
        val validSource = meta.source == "BNR_OFFICIAL"
        val validStatus = meta.status == "OFFICIAL"
        val validRate = meta.rate != null && meta.rate!! > 0 && !meta.rate!!.isNaN() && !meta.rate!!.isInfinite()
        val effDate = meta.effectiveDate
        val validDate = !effDate.isNullOrBlank() && effDate <= transactionDate
        return validSource && validStatus && validRate && validDate
    }

    fun isValidExchangeRateStatus(status: String?): Boolean {
        return status != null && status in listOf("OFFICIAL", "PENDING", "FAILED", "UNVERIFIED")
    }

    fun isValidExchangeRateDto(dto: ExchangeRateDto): Boolean {
        val status = dto.status ?: return false
        if (!isValidExchangeRateStatus(status)) return false
        val rate = dto.rate ?: dto.rates?.get("EUR") ?: return false
        if (rate <= 0 || rate.isNaN() || rate.isInfinite()) return false

        if (status == "OFFICIAL") {
            if (dto.source != "BNR_OFFICIAL") return false
        }
        return true
    }
}

fun TransactionDto.toEntity(documentId: String? = null): TransactionEntity? {
    val id = transactionId?.takeIf { it.isNotBlank() } ?: documentId?.takeIf { it.isNotBlank() } ?: return null
    val date = transactionDate?.takeIf { it.isNotBlank() } ?: return null
    if (!FirestoreDtoValidator.isValidTransactionType(type)) return null
    if (!FirestoreDtoValidator.isValidAccount(account)) return null
    if (!FirestoreDtoValidator.isValidDestination(type, destination)) return null
    val cat = category?.takeIf { it.isNotBlank() } ?: return null

    val ron = amountRon ?: return null
    if (ron.isNaN() || ron.isInfinite()) return null

    val eur = amountEur ?: return null
    if (eur.isNaN() || eur.isInfinite()) return null

    val rate = exchangeRate ?: return null
    if (rate <= 0 || rate.isNaN() || rate.isInfinite()) return null

    val isOfficialMeta = FirestoreDtoValidator.isValidConversionMetadata(exchangeRateMetadata, date)
    val finalSource: String
    val finalStatus: String

    if (isOfficialMeta) {
        finalSource = "BNR_OFFICIAL"
        finalStatus = "OFFICIAL"
    } else {
        finalSource = "UNVERIFIED"
        finalStatus = "UNVERIFIED"
    }

    val finalRateDate = exchangeRateMetadata?.effectiveDate?.takeIf { it.isNotBlank() }
        ?: exchangeRateDate?.takeIf { it.isNotBlank() }
        ?: date

    return TransactionEntity(
        id = id,
        userId = createdByUid?.takeIf { it.isNotBlank() } ?: "remote_user",
        date = date,
        description = description ?: "",
        amountRON = ron,
        amountEUR = eur,
        exchangeRate = rate,
        exchangeRateDate = finalRateDate,
        type = type!!,
        account = account!!,
        category = cat,
        subCategory = subCategory ?: "",
        destination = destination,
        createdAt = createdAt ?: System.currentTimeMillis(),
        updatedAt = updatedAt ?: System.currentTimeMillis(),
        exchangeRateSource = finalSource,
        conversionStatus = finalStatus,
        categoryId = categoryId,
        subCategoryId = subCategoryId,
        syncStatus = "SYNCED",
        lastSyncedAt = System.currentTimeMillis(),
        isDeleted = isDeleted == true,
        householdId = householdId,
        createdByUid = createdByUid
    )
}

fun CategoryDto.toEntity(documentId: String? = null): CategoryEntity? {
    val id = categoryId?.takeIf { it.isNotBlank() } ?: documentId?.takeIf { it.isNotBlank() } ?: return null
    val categoryName = name?.takeIf { it.isNotBlank() } ?: return null
    if (!FirestoreDtoValidator.isValidTransactionType(type)) return null

    return CategoryEntity(
        id = id,
        name = categoryName,
        type = type!!,
        subCategory = subCategory ?: "",
        userId = createdByUid?.takeIf { it.isNotBlank() } ?: "remote_user",
        createdAt = createdAt ?: System.currentTimeMillis(),
        updatedAt = updatedAt ?: System.currentTimeMillis(),
        isDeleted = isDeleted == true,
        syncStatus = "SYNCED",
        householdId = householdId
    )
}

fun ExchangeRateDto.toEntity(documentId: String? = null): ExchangeRateEntity? {
    val reqDate = requestedDate?.takeIf { it.isNotBlank() } ?: documentId?.takeIf { it.isNotBlank() } ?: return null
    val effDate = effectiveDate?.takeIf { it.isNotBlank() } ?: reqDate
    val rateVal = rate ?: rates?.get("EUR") ?: return null
    if (rateVal <= 0 || rateVal.isNaN() || rateVal.isInfinite()) return null

    val rawStatus = status?.takeIf { FirestoreDtoValidator.isValidExchangeRateStatus(it) } ?: "UNVERIFIED"
    val rawSource = source?.takeIf { it.isNotBlank() } ?: "UNVERIFIED"

    // Maintain strict non-official rules: never upgrade questionable metadata to OFFICIAL
    val finalStatus: String
    val finalSource: String
    if (rawStatus == "OFFICIAL" && rawSource == "BNR_OFFICIAL") {
        finalStatus = "OFFICIAL"
        finalSource = "BNR_OFFICIAL"
    } else {
        finalStatus = if (rawStatus == "OFFICIAL") "UNVERIFIED" else rawStatus
        finalSource = if (rawSource == "BNR_OFFICIAL" && rawStatus != "OFFICIAL") "UNVERIFIED" else rawSource
    }

    return ExchangeRateEntity(
        date = reqDate,
        requestedDate = reqDate,
        effectiveDate = effDate,
        rate = rateVal,
        source = finalSource,
        fetchedAt = fetchedAt ?: System.currentTimeMillis(),
        status = finalStatus
    )
}

fun TransactionEntity.toFirestoreMap(householdId: String, userUid: String? = null, migrationId: String? = null): Map<String, Any?> {
    val effectiveUid = if (userId.isNotBlank() && userId != "local_user") userId else (userUid ?: "remote_user")
    val serializedCreatedByUid = createdByUid?.takeIf { it.isNotBlank() } ?: effectiveUid
    val effectiveRateDate = exchangeRateDate.ifBlank { date }
    val isOfficialRate = conversionStatus == "OFFICIAL" && exchangeRate > 0.0 && exchangeRateSource == "BNR_OFFICIAL"
    val validMetadata = if (isOfficialRate) {
        mapOf(
            "source" to exchangeRateSource,
            "status" to conversionStatus,
            "rate" to exchangeRate,
            "effectiveDate" to effectiveRateDate
        )
    } else {
        null
    }
    val baseMap = mutableMapOf<String, Any?>(
        "transactionId" to id,
        "householdId" to householdId,
        "createdByUid" to serializedCreatedByUid,
        "transactionDate" to date,
        "description" to description,
        "amountRon" to amountRON,
        "amountEur" to amountEUR,
        "exchangeRate" to exchangeRate,
        "exchangeRateDate" to effectiveRateDate,
        "type" to type,
        "account" to account,
        "category" to category,
        "subCategory" to subCategory,
        "destination" to destination,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "exchangeRateSource" to exchangeRateSource,
        "conversionStatus" to conversionStatus,
        "categoryId" to categoryId,
        "subCategoryId" to subCategoryId,
        "isDeleted" to isDeleted,
        "exchangeRateMetadata" to validMetadata
    )
    if (migrationId != null) {
        baseMap["migrationId"] = migrationId
    }
    return baseMap
}

fun CategoryEntity.toFirestoreMap(householdId: String, userUid: String? = null): Map<String, Any?> {
    val effectiveUid = if (userId.isNotBlank() && userId != "local_user") userId else (userUid ?: "remote_user")
    return mapOf(
        "categoryId" to id,
        "householdId" to householdId,
        "name" to name,
        "type" to type,
        "subCategory" to subCategory,
        "createdByUid" to effectiveUid,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isDeleted" to isDeleted
    )
}

fun ExchangeRateEntity.toFirestoreMap(householdId: String, migrationId: String? = null): Map<String, Any?> {
    val reqDate = requestedDate.ifBlank { date }
    val effDate = effectiveDate.ifBlank { date }
    val baseMap = mutableMapOf<String, Any?>(
        "requestedDate" to reqDate,
        "effectiveDate" to effDate,
        "rate" to rate,
        "source" to source,
        "fetchedAt" to fetchedAt,
        "status" to status,
        "householdId" to householdId,
        "rates" to mapOf("EUR" to rate)
    )
    if (migrationId != null) {
        baseMap["migrationId"] = migrationId
    }
    return baseMap
}


