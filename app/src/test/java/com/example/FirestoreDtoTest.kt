package com.example

import com.example.data.model.CategoryDto
import com.example.data.model.ExchangeRateMetadataDto
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.HouseholdMemberDto
import com.example.data.model.TransactionDto
import com.example.data.model.TransactionEntity
import com.example.data.model.toEntity
import com.example.data.model.toFirestoreMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreDtoTest {

    @Test
    fun testValidTransactionMappingPreservesAmountsAndDates() {
        val dto = TransactionDto(
            transactionId = "tx123",
            householdId = "hh_alpha",
            createdByUid = "user_bubu",
            transactionDate = "2026-08-10",
            description = "Groceries",
            amountRon = 100.0,
            amountEur = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-09",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Supermarket",
            destination = null,
            createdAt = 1700000000000L,
            updatedAt = 1700000500000L,
            exchangeRateMetadata = ExchangeRateMetadataDto(
                source = "BNR_OFFICIAL",
                status = "OFFICIAL",
                rate = 5.0,
                effectiveDate = "2026-08-09"
            ),
            isDeleted = false
        )

        val entity = dto.toEntity("doc_tx123")
        assertNotNull(entity)
        assertEquals("tx123", entity!!.id)
        assertEquals("user_bubu", entity.userId)
        assertEquals("2026-08-10", entity.date)
        assertEquals("Groceries", entity.description)
        assertEquals(100.0, entity.amountRON, 0.0001)
        assertEquals(20.0, entity.amountEUR, 0.0001)
        assertEquals(5.0, entity.exchangeRate, 0.0001)
        assertEquals("2026-08-09", entity.exchangeRateDate)
        assertEquals("Expense", entity.type)
        assertEquals("Card", entity.account)
        assertEquals("Food", entity.category)
        assertEquals("Supermarket", entity.subCategory)
        assertEquals(null, entity.destination)
        assertEquals(1700000000000L, entity.createdAt)
        assertEquals(1700000500000L, entity.updatedAt)
        assertEquals("BNR_OFFICIAL", entity.exchangeRateSource)
        assertEquals("OFFICIAL", entity.conversionStatus)
        assertEquals("SYNCED", entity.syncStatus)
        assertEquals(false, entity.isDeleted)
        assertEquals("hh_alpha", entity.householdId)
        assertEquals("user_bubu", entity.createdByUid)
    }

    @Test
    fun testValidCategoryMappingPreservesFieldsAndTombstone() {
        val dto = CategoryDto(
            categoryId = "cat_456",
            householdId = "hh_alpha",
            name = "Salary",
            type = "Income",
            subCategory = "Primary",
            createdByUid = "user_admin",
            createdAt = 1690000000000L,
            updatedAt = 1690000500000L,
            isDeleted = true
        )

        val entity = dto.toEntity("doc_cat_456")
        assertNotNull(entity)
        assertEquals("cat_456", entity!!.id)
        assertEquals("Salary", entity.name)
        assertEquals("Income", entity.type)
        assertEquals("Primary", entity.subCategory)
        assertEquals("user_admin", entity.userId)
        assertEquals(1690000000000L, entity.createdAt)
        assertEquals(1690000500000L, entity.updatedAt)
        assertTrue(entity.isDeleted)
        assertEquals("SYNCED", entity.syncStatus)
    }

    @Test
    fun testInvalidConversionMetadataDowngradedToUnverified() {
        // Effective date in future relative to transactionDate -> invalid official metadata
        val dto = TransactionDto(
            transactionId = "tx999",
            householdId = "hh_alpha",
            createdByUid = "user_123",
            transactionDate = "2026-08-10",
            description = "Test invalid rate date",
            amountRon = 50.0,
            amountEur = 10.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-15", // After transaction date
            type = "Income",
            account = "Cash",
            category = "Bonus",
            destination = "Bubu",
            exchangeRateMetadata = ExchangeRateMetadataDto(
                source = "BNR_OFFICIAL",
                status = "OFFICIAL",
                rate = 5.0,
                effectiveDate = "2026-08-15" // Invalid: after transaction date 2026-08-10
            )
        )

        val entity = dto.toEntity()
        assertNotNull(entity)
        assertEquals("UNVERIFIED", entity!!.exchangeRateSource)
        assertEquals("UNVERIFIED", entity.conversionStatus)
        // Ensure downloaded amounts are preserved exactly
        assertEquals(50.0, entity.amountRON, 0.0001)
        assertEquals(10.0, entity.amountEUR, 0.0001)
        assertEquals(5.0, entity.exchangeRate, 0.0001)
    }

    @Test
    fun testMalformedRequiredFieldsRejected() {
        val baseDto = TransactionDto(
            transactionId = "tx_bad",
            householdId = "hh_alpha",
            createdByUid = "user_123",
            transactionDate = "2026-08-10",
            description = "Bad tx",
            amountRon = 50.0,
            amountEur = 10.0,
            exchangeRate = 5.0,
            type = "Expense",
            account = "Card",
            category = "General"
        )

        // Invalid transaction type
        assertNull(baseDto.copy(type = "InvalidType").toEntity())

        // Invalid account
        assertNull(baseDto.copy(account = "CryptoAccount").toEntity())

        // Invalid destination for Expense
        assertNull(baseDto.copy(destination = "Bubu").toEntity())

        // Missing transactionDate
        assertNull(baseDto.copy(transactionDate = "").toEntity())

        // Missing category
        assertNull(baseDto.copy(category = "").toEntity())

        // Invalid zero/negative rate
        assertNull(baseDto.copy(exchangeRate = 0.0).toEntity())

        // Invalid Category Type
        val badCatDto = CategoryDto(
            categoryId = "c1",
            name = "Test",
            type = "UnknownType"
        )
        assertNull(badCatDto.toEntity())
    }

    @Test
    fun testValidIncomeDestinationsAllowed() {
        val dtoBubu = TransactionDto(
            transactionId = "tx_inc_1",
            transactionDate = "2026-08-10",
            amountRon = 100.0,
            amountEur = 20.0,
            exchangeRate = 5.0,
            type = "Income",
            account = "Cash",
            category = "Gift",
            destination = "Bubu"
        )
        assertNotNull(dtoBubu.toEntity())

        val dtoPiticania = dtoBubu.copy(destination = "Piticania")
        assertNotNull(dtoPiticania.toEntity())

        val dtoNullDest = dtoBubu.copy(destination = null)
        assertNotNull(dtoNullDest.toEntity())

        val dtoInvalidDest = dtoBubu.copy(destination = "UnknownDest")
        assertNull(dtoInvalidDest.toEntity())
    }

    @Test
    fun testToFirestoreMapOmitsMigrationIdWhenNull() {
        val tx = TransactionEntity(
            id = "tx_normal_01",
            userId = "user_auth_123",
            date = "2026-08-20",
            description = "Normal Coffee",
            amountRON = 15.0,
            amountEUR = 3.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Food & Dining",
            subCategory = "Cafes",
            destination = null,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL"
        )

        val payload = tx.toFirestoreMap(householdId = "hh_alpha", migrationId = null)

        // Must NOT contain key "migrationId" when migrationId is null
        assertFalse("Payload must NOT contain migrationId key when null", payload.containsKey("migrationId"))
        assertNull(payload["migrationId"])

        // Existing fields must remain intact
        assertEquals("tx_normal_01", payload["transactionId"])
        assertEquals("hh_alpha", payload["householdId"])
        assertEquals("user_auth_123", payload["createdByUid"])
        assertEquals("2026-08-20", payload["transactionDate"])
        assertEquals("Normal Coffee", payload["description"])
        assertEquals(15.0, payload["amountRon"])
        assertEquals(3.0, payload["amountEur"])
        assertEquals(5.0, payload["exchangeRate"])
        assertEquals("Expense", payload["type"])
        assertEquals("Card", payload["account"])
        assertEquals("Food & Dining", payload["category"])
        assertEquals("Cafes", payload["subCategory"])
        assertEquals(false, payload["isDeleted"])
    }

    @Test
    fun testToFirestoreMapIncludesMigrationIdWhenPresent() {
        val tx = TransactionEntity(
            id = "tx_mig_01",
            userId = "user_auth_123",
            date = "2026-08-20",
            description = "Migration Item",
            amountRON = 50.0,
            amountEUR = 10.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Cash",
            category = "General",
            subCategory = "",
            destination = null,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL"
        )

        val payload = tx.toFirestoreMap(householdId = "hh_alpha", migrationId = "mig_session_999")

        // Must contain key "migrationId" and match the provided session ID
        assertTrue("Payload MUST contain migrationId key when provided", payload.containsKey("migrationId"))
        assertEquals("mig_session_999", payload["migrationId"])

        // Existing fields must remain intact
        assertEquals("tx_mig_01", payload["transactionId"])
        assertEquals("hh_alpha", payload["householdId"])
        assertEquals("user_auth_123", payload["createdByUid"])
        assertEquals(50.0, payload["amountRon"])
        assertEquals(10.0, payload["amountEur"])
    }

    @Test
    fun testExchangeRateToFirestoreMapOmitsMigrationIdWhenNull() {
        val rate = ExchangeRateEntity(
            date = "2026-08-27",
            requestedDate = "2026-08-27",
            effectiveDate = "2026-08-27",
            rate = 4.9765,
            source = "BNR_OFFICIAL",
            fetchedAt = 1700000000000L,
            status = "OFFICIAL"
        )

        val payload = rate.toFirestoreMap(householdId = "hh_alpha", migrationId = null)

        // Must NOT contain key "migrationId" when migrationId is null
        assertFalse("Payload must NOT contain migrationId key when null", payload.containsKey("migrationId"))
        assertNull(payload["migrationId"])
        assertEquals("2026-08-27", payload["requestedDate"])
        assertEquals("2026-08-27", payload["effectiveDate"])
        assertEquals(4.9765, payload["rate"])
        assertEquals("BNR_OFFICIAL", payload["source"])
        assertEquals("OFFICIAL", payload["status"])
        assertEquals("hh_alpha", payload["householdId"])
    }

    @Test
    fun testExchangeRateToFirestoreMapIncludesMigrationIdWhenPresent() {
        val rate = ExchangeRateEntity(
            date = "2026-08-27",
            requestedDate = "2026-08-27",
            effectiveDate = "2026-08-27",
            rate = 4.9765,
            source = "BNR_OFFICIAL",
            fetchedAt = 1700000000000L,
            status = "OFFICIAL"
        )

        val payload = rate.toFirestoreMap(householdId = "hh_alpha", migrationId = "mig_session_888")

        // Must contain key "migrationId" and match the provided session ID
        assertTrue("Payload MUST contain migrationId key when provided", payload.containsKey("migrationId"))
        assertEquals("mig_session_888", payload["migrationId"])
        assertEquals("2026-08-27", payload["requestedDate"])
        assertEquals(4.9765, payload["rate"])
    }

    @Test
    fun testTransactionToFirestoreMapSafelyHandlesPendingOrZeroRateMetadata() {
        val txPending = TransactionEntity(
            id = "tx_pending_01",
            userId = "user_auth_123",
            date = "2026-08-27",
            description = "Offline Coffee",
            amountRON = 20.0,
            amountEUR = 0.0,
            exchangeRate = 0.0,
            exchangeRateDate = "2026-08-27",
            type = "Expense",
            account = "Card",
            category = "Food & Dining",
            subCategory = "Cafes",
            destination = null,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
            exchangeRateSource = "NONE",
            conversionStatus = "PENDING"
        )

        val payload = txPending.toFirestoreMap(householdId = "hh_alpha", migrationId = null)

        // For pending/zero rate transactions, exchangeRateMetadata must be null so that
        // firestore.rules evaluates meta == null (which is permitted) rather than meta.rate > 0 (which would fail on 0.0)
        assertNull("exchangeRateMetadata must be null for pending/zero-rate transactions", payload["exchangeRateMetadata"])
        assertEquals(0.0, payload["exchangeRate"])
        assertEquals("PENDING", payload["conversionStatus"])
    }

    @Test
    fun testHouseholdMemberDtoSerializationRoundTripWithInviteId() {
        val memberDto = HouseholdMemberDto(
            uid = "user_invitee_456",
            email = "invitee@example.com",
            displayName = "Invited User",
            role = "member",
            status = "ACTIVE",
            joinedAt = 1750000000000L,
            invitedByUid = "user_owner_123",
            inviteId = "inv_session_789"
        )

        val map = memberDto.toMap()
        assertEquals("user_invitee_456", map["uid"])
        assertEquals("invitee@example.com", map["email"])
        assertEquals("Invited User", map["displayName"])
        assertEquals("member", map["role"])
        assertEquals("ACTIVE", map["status"])
        assertEquals(1750000000000L, map["joinedAt"])
        assertEquals("user_owner_123", map["invitedByUid"])
        assertEquals("inv_session_789", map["inviteId"])
        assertTrue(map.containsKey("inviteId"))

        val deserialized = HouseholdMemberDto.fromMap(map, "user_invitee_456")
        assertEquals("user_invitee_456", deserialized.uid)
        assertEquals("invitee@example.com", deserialized.email)
        assertEquals("Invited User", deserialized.displayName)
        assertEquals("member", deserialized.role)
        assertEquals("ACTIVE", deserialized.status)
        assertEquals(1750000000000L, deserialized.joinedAt)
        assertEquals("user_owner_123", deserialized.invitedByUid)
        assertEquals("inv_session_789", deserialized.inviteId)
    }

    @Test
    fun testHouseholdMemberDtoSerializationOmitsInviteIdWhenNull() {
        val ownerMemberDto = HouseholdMemberDto(
            uid = "user_owner_123",
            email = "owner@example.com",
            displayName = "Owner User",
            role = "owner",
            status = "ACTIVE",
            joinedAt = 1750000000000L,
            invitedByUid = null,
            inviteId = null
        )

        val map = ownerMemberDto.toMap()
        assertEquals("user_owner_123", map["uid"])
        assertEquals("owner@example.com", map["email"])
        assertEquals("owner", map["role"])
        assertEquals("ACTIVE", map["status"])
        assertFalse("inviteId must be omitted from toMap() when null", map.containsKey("inviteId"))
        assertNull(map["inviteId"])

        // Test backward compatibility: deserializing legacy document without inviteId key
        val legacyDocMap = mapOf<String, Any?>(
            "uid" to "legacy_user",
            "email" to "legacy@example.com",
            "role" to "member",
            "status" to "ACTIVE"
        )
        val deserializedLegacy = HouseholdMemberDto.fromMap(legacyDocMap, "legacy_user")
        assertEquals("legacy_user", deserializedLegacy.uid)
        assertEquals("legacy@example.com", deserializedLegacy.email)
        assertEquals("member", deserializedLegacy.role)
        assertEquals("ACTIVE", deserializedLegacy.status)
        assertNull(deserializedLegacy.inviteId)
    }
}
