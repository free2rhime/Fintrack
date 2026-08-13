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
        isDeleted = isDeleted == true
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
        syncStatus = "SYNCED"
    )
}
