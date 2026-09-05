package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.toFirestoreMap
import com.example.data.repository.RoomCategoryRepository
import com.example.data.repository.RoomTransactionRepository
import com.example.data.util.CsvDuplicateMode
import com.example.data.util.CsvImportOrchestrator
import com.example.data.util.CsvImportParseResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CsvCategoryDeduplicationTest {

    private lateinit var context: Context
    private lateinit var db: FinTrackDatabase
    private lateinit var roomCatRepo: RoomCategoryRepository
    private lateinit var roomTxRepo: RoomTransactionRepository
    private lateinit var cacheDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val exchangeRateService = com.example.data.service.ExchangeRateService(db.exchangeRateDao())
        roomCatRepo = RoomCategoryRepository(
            categoryDao = db.categoryDao(),
            syncOutboxDao = db.syncOutboxDao(),
            database = db,
            transactionDao = db.transactionDao()
        )
        roomTxRepo = RoomTransactionRepository(
            transactionDao = db.transactionDao(),
            exchangeRateService = exchangeRateService,
            exchangeRateDao = db.exchangeRateDao(),
            syncOutboxDao = db.syncOutboxDao(),
            database = db
        )
        cacheDir = context.cacheDir
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun test1_existingHouseholdCategoryIsReusedDuringCsvImport() = runBlocking {
        val householdId = "hh_test_1"
        val userId = "user_1"

        // Seed existing category for household
        val existingCat = CategoryEntity(
            id = "cat_utilitati_1",
            name = "🏠 Utilitati",
            type = "Expense",
            subCategory = "⛽ Gaz",
            userId = userId,
            householdId = householdId
        )
        db.categoryDao().insertCategory(existingCat)

        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
tx_1,2026-08-10,150.0,Factura gaz,Expense,Card,🏠 Utilitati,⛽ Gaz
"""
        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)
        val parseResult = orchestrator.parseAndValidateFromContent(
            csvContent = csv,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        ) as CsvImportParseResult.Success

        assertEquals(0, parseResult.preview.missingCategories.size)

        val importResult = orchestrator.executeImport(
            preview = parseResult.preview,
            cacheDir = cacheDir,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        )

        assertTrue(importResult.success)
        assertEquals(0, importResult.categoriesCreatedCount)
        assertEquals(0, importResult.subcategoriesCreatedCount)

        val allCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, allCats.size)
        assertEquals("cat_utilitati_1", allCats.first().id)
    }

    @Test
    fun test2_existingHouseholdSubcategoryIsReused() = runBlocking {
        val householdId = "hh_test_2"
        val userId = "user_2"

        val existingCat = CategoryEntity(
            id = "cat_digi_1",
            name = "🏠 Utilitati",
            type = "Expense",
            subCategory = "📶 Digi",
            userId = userId,
            householdId = householdId
        )
        db.categoryDao().insertCategory(existingCat)

        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
tx_digi_1,2026-08-10,80.0,Abonament Digi,Expense,Card,🏠 Utilitati,📶 Digi
"""
        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)
        val parseResult = orchestrator.parseAndValidateFromContent(
            csvContent = csv,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        ) as CsvImportParseResult.Success

        assertEquals(0, parseResult.preview.missingCategories.size)

        val importResult = orchestrator.executeImport(
            preview = parseResult.preview,
            cacheDir = cacheDir,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        )

        assertTrue(importResult.success)
        assertEquals(0, importResult.subcategoriesCreatedCount)

        val allCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, allCats.size)
        assertEquals("cat_digi_1", allCats.first().id)
    }

    @Test
    fun test3_importingSameCsvTwiceDoesNotCreateDuplicateCategories() = runBlocking {
        val householdId = "hh_test_3"
        val userId = "user_3"

        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
tx_dupl_1,2026-08-10,120.0,Enel,Expense,Card,🏠 Utilitati,⚡ Electricitate
tx_dupl_2,2026-08-11,60.0,Apa Nova,Expense,Card,🏠 Utilitati,💧 Consum
"""
        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)

        // First import
        val parse1 = orchestrator.parseAndValidateFromContent(
            csvContent = csv,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        ) as CsvImportParseResult.Success

        assertEquals(2, parse1.preview.missingCategories.size)
        val import1 = orchestrator.executeImport(
            preview = parse1.preview,
            cacheDir = cacheDir,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        )
        assertTrue(import1.success)
        assertEquals(1, import1.categoriesCreatedCount) // 1 distinct category group: "🏠 Utilitati"
        assertEquals(2, import1.subcategoriesCreatedCount) // 2 subcategories

        // Verify categories count after 1st import
        val catsAfterFirst = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(2, catsAfterFirst.size)

        // Second import of the same CSV
        val parse2 = orchestrator.parseAndValidateFromContent(
            csvContent = csv,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        ) as CsvImportParseResult.Success

        assertEquals(0, parse2.preview.missingCategories.size)
        val import2 = orchestrator.executeImport(
            preview = parse2.preview,
            cacheDir = cacheDir,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        )
        assertTrue(import2.success)
        assertEquals(0, import2.categoriesCreatedCount)
        assertEquals(0, import2.subcategoriesCreatedCount)

        // Verify categories count remains exactly 2 (NO duplicates)
        val catsAfterSecond = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(2, catsAfterSecond.size)
    }

    @Test
    fun test4_importingSameCsvTwiceDoesNotCreateDuplicateSubcategories() = runBlocking {
        val householdId = "hh_test_4"
        val userId = "user_4"

        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
tx_gas_1,2026-08-10,200.0,Gaz 1,Expense,Card,🏠 Utilitati,⛽ Gaz
tx_gas_2,2026-08-11,210.0,Gaz 2,Expense,Card,🏠 Utilitati,⛽ Gaz
"""
        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)

        val parse1 = orchestrator.parseAndValidateFromContent(
            csvContent = csv,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        ) as CsvImportParseResult.Success

        val import1 = orchestrator.executeImport(
            preview = parse1.preview,
            cacheDir = cacheDir,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        )
        assertTrue(import1.success)
        assertEquals(1, import1.categoriesCreatedCount)
        assertEquals(1, import1.subcategoriesCreatedCount)

        val cats1 = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, cats1.size)

        // Second import
        val parse2 = orchestrator.parseAndValidateFromContent(
            csvContent = csv,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        ) as CsvImportParseResult.Success

        val import2 = orchestrator.executeImport(
            preview = parse2.preview,
            cacheDir = cacheDir,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        )
        assertTrue(import2.success)
        assertEquals(0, import2.categoriesCreatedCount)
        assertEquals(0, import2.subcategoriesCreatedCount)

        val cats2 = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, cats2.size)
    }

    @Test
    fun test5_sameCategoryNameInDifferentHouseholdsRemainsSeparate() = runBlocking {
        val hhA = "household_A"
        val hhB = "household_B"
        val userA = "user_A"
        val userB = "user_B"

        val catA = CategoryEntity(
            id = "cat_hh_a",
            name = "🏠 Utilitati",
            type = "Expense",
            subCategory = "⛽ Gaz",
            userId = userA,
            householdId = hhA
        )
        db.categoryDao().insertCategory(catA)

        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
tx_b,2026-08-10,150.0,Gaz HH B,Expense,Card,🏠 Utilitati,⛽ Gaz
"""
        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)
        val parseResult = orchestrator.parseAndValidateFromContent(
            csvContent = csv,
            householdId = hhB,
            userId = userB,
            createdByUid = userB
        ) as CsvImportParseResult.Success

        // For Household B, this category is missing because HH A's categories cannot be reused
        assertEquals(1, parseResult.preview.missingCategories.size)

        val importResult = orchestrator.executeImport(
            preview = parseResult.preview,
            cacheDir = cacheDir,
            householdId = hhB,
            userId = userB,
            createdByUid = userB
        )
        assertTrue(importResult.success)
        assertEquals(1, importResult.categoriesCreatedCount)

        // Verify isolation
        val catsA = db.categoryDao().getAllCategoriesList(hhA)
        val catsB = db.categoryDao().getAllCategoriesList(hhB)
        assertEquals(1, catsA.size)
        assertEquals(1, catsB.size)
        assertEquals("cat_hh_a", catsA.first().id)
        assertFalse(catsB.first().id == "cat_hh_a")
        assertEquals(hhB, catsB.first().householdId)
    }

    @Test
    fun test6_sameCategoryNameWithDifferentExpenseIncomeTypeRemainsSeparate() = runBlocking {
        val householdId = "hh_test_6"
        val userId = "user_6"

        val expenseCat = CategoryEntity(
            id = "cat_expense_1",
            name = "Investitii",
            type = "Expense",
            subCategory = "Actiuni",
            userId = userId,
            householdId = householdId
        )
        db.categoryDao().insertCategory(expenseCat)

        val csvIncome = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory,Destination
tx_inc_1,2026-08-10,500.0,Dividende,Income,Card,Investitii,Dividende,Bubu
"""
        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)
        val parseResult = orchestrator.parseAndValidateFromContent(
            csvContent = csvIncome,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        ) as CsvImportParseResult.Success

        // Must treat Income Investitii as missing because Expense Investitii has different type
        assertEquals(1, parseResult.preview.missingCategories.size)
        assertEquals("Income", parseResult.preview.missingCategories.first().type)

        val importResult = orchestrator.executeImport(
            preview = parseResult.preview,
            cacheDir = cacheDir,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        )
        assertTrue(importResult.success)

        val allCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(2, allCats.size)
        assertTrue(allCats.any { it.type == "Expense" && it.name == "Investitii" && it.id == "cat_expense_1" })
        assertTrue(allCats.any { it.type == "Income" && it.name == "Investitii" })
    }

    @Test
    fun test7_sameSubcategoryNameUnderDifferentParentCategoriesIsRejectedOrDistinct() = runBlocking {
        val householdId = "hh_test_7"
        val userId = "user_7"

        val catUtilitati = CategoryEntity(
            id = "cat_util",
            name = "Utilitati",
            type = "Expense",
            subCategory = "Gaz",
            userId = userId,
            householdId = householdId
        )
        db.categoryDao().insertCategory(catUtilitati)

        // Attempt to import "Gaz" under parent category "Transport"
        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
tx_transp_1,2026-08-10,200.0,Alimentare Gaz,Expense,Card,Transport,Gaz
"""
        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)
        val parseResult = orchestrator.parseAndValidateFromContent(
            csvContent = csv,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        ) as CsvImportParseResult.Success

        // Conflicting subcategory parent is rejected as invalid row
        assertEquals(1, parseResult.preview.invalidRowsCount)
        assertEquals(0, parseResult.preview.validRowsCount)
        assertTrue(parseResult.preview.rowErrors.first().message.contains("belongs to category 'Utilitati'"))
    }

    @Test
    fun test8_existingCategoryIdIsPreservedWhenReused() = runBlocking {
        val householdId = "hh_test_8"
        val userId = "user_8"

        val stableId = "stable_cat_id_12345"
        val existingCat = CategoryEntity(
            id = stableId,
            name = "Food",
            type = "Expense",
            subCategory = "Groceries",
            userId = userId,
            householdId = householdId
        )
        db.categoryDao().insertCategory(existingCat)

        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
tx_food_1,2026-08-10,50.0,Kaufland,Expense,Card,Food,Groceries
"""
        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)
        val parseResult = orchestrator.parseAndValidateFromContent(
            csvContent = csv,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        ) as CsvImportParseResult.Success

        val importResult = orchestrator.executeImport(
            preview = parseResult.preview,
            cacheDir = cacheDir,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        )
        assertTrue(importResult.success)

        val cats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, cats.size)
        assertEquals(stableId, cats.first().id)
    }

    @Test
    fun test9_existingSubcategoryIdIsPreservedWhenReused() = runBlocking {
        val householdId = "hh_test_9"
        val userId = "user_9"

        val stableId = "subcat_electricitate_999"
        val existingCat = CategoryEntity(
            id = stableId,
            name = "🏠 Utilitati",
            type = "Expense",
            subCategory = "⚡ Electricitate",
            userId = userId,
            householdId = householdId
        )
        db.categoryDao().insertCategory(existingCat)

        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
tx_elec_1,2026-08-10,250.0,Enel Energie,Expense,Card,🏠 Utilitati,⚡ Electricitate
"""
        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)
        val parseResult = orchestrator.parseAndValidateFromContent(
            csvContent = csv,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        ) as CsvImportParseResult.Success

        val importResult = orchestrator.executeImport(
            preview = parseResult.preview,
            cacheDir = cacheDir,
            householdId = householdId,
            userId = userId,
            createdByUid = userId
        )
        assertTrue(importResult.success)

        val cats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, cats.size)
        assertEquals(stableId, cats.first().id)
    }

    @Test
    fun test10_33TransactionCsvRegressionRemainsValidWithDeduplication() = runBlocking {
        val testHouseholdId = "hh_33_regression"
        val testUserUid = "user_33_regression"

        // Seed 3 existing categories for this household
        db.categoryDao().insertCategory(CategoryEntity(id = "cat_sal", name = "Salary", type = "Income", subCategory = "Main Job", userId = testUserUid, householdId = testHouseholdId, createdByUid = testUserUid))
        db.categoryDao().insertCategory(CategoryEntity(id = "cat_food", name = "Food & Dining", type = "Expense", subCategory = "Groceries", userId = testUserUid, householdId = testHouseholdId, createdByUid = testUserUid))
        db.categoryDao().insertCategory(CategoryEntity(id = "cat_util", name = "Housing & Utilities", type = "Expense", subCategory = "Electricity", userId = testUserUid, householdId = testHouseholdId, createdByUid = testUserUid))

        // Pre-insert exchange rate
        db.exchangeRateDao().insertRate(
            ExchangeRateEntity(
                date = "2026-08-10",
                requestedDate = "2026-08-10",
                effectiveDate = "2026-08-10",
                rate = 5.0,
                source = "BNR_OFFICIAL",
                status = "OFFICIAL",
                fetchedAt = System.currentTimeMillis()
            )
        )

        // Build 33 transactions
        val sb = StringBuilder()
        sb.append("Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Requested_Rate_Date,Effective_BNR_Rate_Date,Exchange_Rate_Source,Conversion_Status,Description,Type,Account,Category,SubCategory,Destination\n")
        for (i in 1..33) {
            val type = if (i % 5 == 0) "Income" else "Expense"
            val cat = if (type == "Income") "Salary" else if (i % 2 == 0) "Food & Dining" else "Housing & Utilities"
            val sub = if (type == "Income") "Main Job" else if (i % 2 == 0) "Groceries" else "Electricity"
            val dest = if (type == "Income") "Bubu" else ""
            sb.append("TX_REGRESS_$i,2026-08-10,${100.0 * i},${20.0 * i},5.0,2026-08-10,2026-08-10,BNR_OFFICIAL,OFFICIAL,Transaction $i,$type,Card,$cat,$sub,$dest\n")
        }

        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)
        val parseResult = orchestrator.parseAndValidateFromContent(
            csvContent = sb.toString(),
            householdId = testHouseholdId,
            userId = testUserUid,
            createdByUid = testUserUid
        ) as CsvImportParseResult.Success

        assertEquals(33, parseResult.preview.validRowsCount)
        assertEquals(0, parseResult.preview.missingCategories.size)

        val importResult = orchestrator.executeImport(
            preview = parseResult.preview,
            cacheDir = cacheDir,
            householdId = testHouseholdId,
            userId = testUserUid,
            createdByUid = testUserUid
        )

        assertTrue(importResult.success)
        assertEquals(33, importResult.insertedCount)
        assertEquals(0, importResult.categoriesCreatedCount)
        assertEquals(0, importResult.subcategoriesCreatedCount)

        // Check categories count remains exactly 3 (NO duplicates)
        val allCats = db.categoryDao().getAllCategoriesList(testHouseholdId)
        assertEquals(3, allCats.size)

        // Check 33 transactions
        val txs = db.transactionDao().getAllTransactions(testHouseholdId).first()
        assertEquals(33, txs.size)
    }

    @Test
    fun test11_authenticatedHouseholdContextRemainsPresent() = runBlocking {
        val testHouseholdId = "hh_auth_ctx_11"
        val testUserUid = "user_auth_ctx_11"

        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
tx_auth_1,2026-08-10,100.0,Test desc,Expense,Card,TestCat,TestSub
"""
        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)
        val parseResult = orchestrator.parseAndValidateFromContent(
            csvContent = csv,
            householdId = testHouseholdId,
            userId = testUserUid,
            createdByUid = testUserUid
        ) as CsvImportParseResult.Success

        val importResult = orchestrator.executeImport(
            preview = parseResult.preview,
            cacheDir = cacheDir,
            householdId = testHouseholdId,
            userId = testUserUid,
            createdByUid = testUserUid
        )
        assertTrue(importResult.success)

        val createdCat = db.categoryDao().getAllCategoriesList(testHouseholdId).first()
        assertEquals(testHouseholdId, createdCat.householdId)
        assertEquals(testUserUid, createdCat.userId)
        assertEquals(testUserUid, createdCat.createdByUid)

        val createdTx = db.transactionDao().getAllTransactions(testHouseholdId).first().first()
        assertEquals(testHouseholdId, createdTx.householdId)
        assertEquals(testUserUid, createdTx.userId)
        assertEquals(testUserUid, createdTx.createdByUid)
    }

    @Test
    fun test12_outboxEntriesContinueToReferenceValidTransactionAndCategoryRelationships() = runBlocking {
        val testHouseholdId = "hh_outbox_12"
        val testUserUid = "user_outbox_12"

        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
tx_outbox_1,2026-08-10,180.0,Outbox test,Expense,Card,NewCat,NewSub
"""
        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)
        val parseResult = orchestrator.parseAndValidateFromContent(
            csvContent = csv,
            householdId = testHouseholdId,
            userId = testUserUid,
            createdByUid = testUserUid
        ) as CsvImportParseResult.Success

        val importResult = orchestrator.executeImport(
            preview = parseResult.preview,
            cacheDir = cacheDir,
            householdId = testHouseholdId,
            userId = testUserUid,
            createdByUid = testUserUid
        )
        assertTrue(importResult.success)

        val outbox = db.syncOutboxDao().getPendingEntries()
        val catOutbox = outbox.find { it.entityType == "CATEGORY" }
        val txOutbox = outbox.find { it.entityType == "TRANSACTION" }

        assertNotNull(catOutbox)
        assertNotNull(txOutbox)
        assertEquals("UPSERT", catOutbox!!.operation)
        assertEquals("UPSERT", txOutbox!!.operation)
        assertEquals("tx_outbox_1", txOutbox.entityId)

        val localCat = db.categoryDao().getCategoryById(catOutbox.entityId)
        assertNotNull(localCat)
        assertEquals("NewCat", localCat!!.name)
        assertEquals("NewSub", localCat.subCategory)
        assertEquals(testHouseholdId, localCat.householdId)
    }
}
