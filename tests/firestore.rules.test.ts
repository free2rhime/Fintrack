import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
  RulesTestEnvironment
} from '@firebase/rules-unit-testing';
import { readFileSync } from 'fs';
import { doc, getDoc, setDoc, updateDoc, deleteDoc } from 'firebase/firestore';

describe('FinTrack Firestore Security Rules', () => {
  let testEnv: RulesTestEnvironment;

  const PROJECT_ID = 'fintrack-emulator-test';
  const HOUSEHOLD_ID = 'household_100';

  const OWNER_UID = 'user_owner';
  const ADMIN_UID = 'user_admin';
  const MEMBER_UID = 'user_member';
  const INACTIVE_UID = 'user_inactive';
  const STRANGER_UID = 'user_stranger';

  before(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        rules: readFileSync('firestore.rules', 'utf8'),
        host: '127.0.0.1',
        port: 8080
      }
    });
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();

    // Seed test environment using admin context (bypasses rules)
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();

      // Create household document
      await setDoc(doc(db, `households/${HOUSEHOLD_ID}`), {
        householdId: HOUSEHOLD_ID,
        name: 'The FinTrack Family',
        createdByUid: OWNER_UID,
        createdAt: '2026-08-01T00:00:00Z'
      });

      // Owner member
      await setDoc(doc(db, `households/${HOUSEHOLD_ID}/members/${OWNER_UID}`), {
        role: 'owner',
        status: 'ACTIVE',
        joinedAt: '2026-08-01T00:00:00Z'
      });

      // Admin member
      await setDoc(doc(db, `households/${HOUSEHOLD_ID}/members/${ADMIN_UID}`), {
        role: 'admin',
        status: 'ACTIVE',
        joinedAt: '2026-08-01T00:00:00Z'
      });

      // Regular member
      await setDoc(doc(db, `households/${HOUSEHOLD_ID}/members/${MEMBER_UID}`), {
        role: 'member',
        status: 'ACTIVE',
        joinedAt: '2026-08-01T00:00:00Z'
      });

      // Inactive member
      await setDoc(doc(db, `households/${HOUSEHOLD_ID}/members/${INACTIVE_UID}`), {
        role: 'member',
        status: 'INACTIVE',
        joinedAt: '2026-08-01T00:00:00Z'
      });

      // Sample existing transaction
      await setDoc(doc(db, `households/${HOUSEHOLD_ID}/transactions/tx_existing`), {
        transactionId: 'tx_existing',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amount: 150.0,
        currency: 'RON',
        transactionDate: '2026-08-10',
        createdByUid: MEMBER_UID,
        createdAt: '2026-08-10T12:00:00Z',
        isDeleted: false
      });
    });
  });

  after(async () => {
    await testEnv.cleanup();
  });

  // --------------------------------------------------------------------------
  // 1. Unauthenticated & Non-Member Access
  // --------------------------------------------------------------------------
  describe('Unauthenticated & Household Boundary Access', () => {
    it('denies unauthenticated read access to household', async () => {
      const unauthDb = testEnv.unauthenticatedContext().firestore();
      await assertFails(getDoc(doc(unauthDb, `households/${HOUSEHOLD_ID}`)));
    });

    it('denies unauthenticated write access to transactions', async () => {
      const unauthDb = testEnv.unauthenticatedContext().firestore();
      await assertFails(setDoc(doc(unauthDb, `households/${HOUSEHOLD_ID}/transactions/tx_unauth`), {
        transactionId: 'tx_unauth',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Cash',
        destination: null,
        createdByUid: 'anonymous',
        transactionDate: '2026-08-12'
      }));
    });

    it('denies read access to non-member (stranger)', async () => {
      const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
      await assertFails(getDoc(doc(strangerDb, `households/${HOUSEHOLD_ID}`)));
      await assertFails(getDoc(doc(strangerDb, `households/${HOUSEHOLD_ID}/transactions/tx_existing`)));
    });

    it('denies read access to inactive household member', async () => {
      const inactiveDb = testEnv.authenticatedContext(INACTIVE_UID).firestore();
      await assertFails(getDoc(doc(inactiveDb, `households/${HOUSEHOLD_ID}/transactions/tx_existing`)));
    });

    it('allows read access to active household member', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(getDoc(doc(memberDb, `households/${HOUSEHOLD_ID}`)));
      await assertSucceeds(getDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_existing`)));
    });
  });

  // --------------------------------------------------------------------------
  // 2. Membership Administration & Self-Join Prevention
  // --------------------------------------------------------------------------
  describe('Membership Administration & Self-Join Prevention', () => {
    it('denies self-join: stranger writing member doc for themselves', async () => {
      const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
      await assertFails(setDoc(doc(strangerDb, `households/${HOUSEHOLD_ID}/members/${STRANGER_UID}`), {
        role: 'member',
        status: 'ACTIVE',
        joinedAt: '2026-08-12T00:00:00Z'
      }));
    });

    it('denies regular member adding a new member', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/members/user_new`), {
        role: 'member',
        status: 'ACTIVE',
        joinedAt: '2026-08-12T00:00:00Z'
      }));
    });

    it('denies regular member updating another member role', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(updateDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/members/${MEMBER_UID}`), {
        role: 'admin'
      }));
    });

    it('allows household owner or admin to add a new member', async () => {
      const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
      await assertSucceeds(setDoc(doc(adminDb, `households/${HOUSEHOLD_ID}/members/user_new`), {
        role: 'member',
        status: 'ACTIVE',
        joinedAt: '2026-08-12T00:00:00Z'
      }));
    });

    it('allows household creator to initialize their owner record on creation', async () => {
      const newHouseholdId = 'household_200';
      const creatorUid = 'user_creator';

      // First create household doc
      const creatorDb = testEnv.authenticatedContext(creatorUid).firestore();
      await assertSucceeds(setDoc(doc(creatorDb, `households/${newHouseholdId}`), {
        householdId: newHouseholdId,
        name: 'New Home',
        createdByUid: creatorUid,
        createdAt: '2026-08-12T00:00:00Z'
      }));

      // Creator initializes owner member doc
      await assertSucceeds(setDoc(doc(creatorDb, `households/${newHouseholdId}/members/${creatorUid}`), {
        role: 'owner',
        status: 'ACTIVE',
        joinedAt: '2026-08-12T00:00:00Z'
      }));
    });
  });

  // --------------------------------------------------------------------------
  // 3. Transaction Field & Domain Validations
  // --------------------------------------------------------------------------
  describe('Transaction Schema & Domain Validations', () => {
    it('allows valid Income transaction with destination Bubu', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_inc_bubu`), {
        transactionId: 'tx_inc_bubu',
        householdId: HOUSEHOLD_ID,
        type: 'Income',
        account: 'Card',
        destination: 'Bubu',
        amount: 5000,
        currency: 'RON',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('allows valid Income transaction with destination Piticania', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_inc_piti`), {
        transactionId: 'tx_inc_piti',
        householdId: HOUSEHOLD_ID,
        type: 'Income',
        account: 'Cash',
        destination: 'Piticania',
        amount: 3000,
        currency: 'RON',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('allows valid Income transaction with destination null', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_inc_null`), {
        transactionId: 'tx_inc_null',
        householdId: HOUSEHOLD_ID,
        type: 'Income',
        account: 'Meal Tickets',
        destination: null,
        amount: 700,
        currency: 'RON',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('denies Income transaction with invalid destination', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_inc_bad_dest`), {
        transactionId: 'tx_inc_bad_dest',
        householdId: HOUSEHOLD_ID,
        type: 'Income',
        account: 'Card',
        destination: 'InvalidDestination',
        amount: 1000,
        currency: 'RON',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('allows valid Expense transaction with null destination', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_exp_ok`), {
        transactionId: 'tx_exp_ok',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amount: 120,
        currency: 'RON',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('denies Expense transaction with non-null destination', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_exp_bad_dest`), {
        transactionId: 'tx_exp_bad_dest',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: 'Bubu',
        amount: 120,
        currency: 'RON',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('denies transaction with invalid type', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_bad_type`), {
        transactionId: 'tx_bad_type',
        householdId: HOUSEHOLD_ID,
        type: 'Invest',
        account: 'Card',
        destination: null,
        amount: 200,
        currency: 'RON',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('denies transaction with invalid account', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_bad_account`), {
        transactionId: 'tx_bad_account',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'CryptoWallet',
        destination: null,
        amount: 200,
        currency: 'RON',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('allows Meal Tickets as account', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_meal_tickets`), {
        transactionId: 'tx_meal_tickets',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Meal Tickets',
        destination: null,
        amount: 45,
        currency: 'RON',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });
  });

  // --------------------------------------------------------------------------
  // 4. Conversion Metadata & Effective Date Rules
  // --------------------------------------------------------------------------
  describe('Conversion Metadata Validation', () => {
    it('allows valid BNR official conversion metadata', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_conv_ok`), {
        transactionId: 'tx_conv_ok',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amount: 100,
        currency: 'EUR',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID,
        exchangeRateMetadata: {
          source: 'BNR_OFFICIAL',
          status: 'OFFICIAL',
          rate: 4.9750,
          effectiveDate: '2026-08-10'
        }
      }));
    });

    it('denies conversion metadata with unofficial source', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_conv_bad_source`), {
        transactionId: 'tx_conv_bad_source',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amount: 100,
        currency: 'EUR',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID,
        exchangeRateMetadata: {
          source: 'CUSTOM_SOURCE',
          status: 'OFFICIAL',
          rate: 4.9750,
          effectiveDate: '2026-08-10'
        }
      }));
    });

    it('denies conversion metadata with negative or zero rate', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_conv_neg_rate`), {
        transactionId: 'tx_conv_neg_rate',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amount: 100,
        currency: 'EUR',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID,
        exchangeRateMetadata: {
          source: 'BNR_OFFICIAL',
          status: 'OFFICIAL',
          rate: -4.9750,
          effectiveDate: '2026-08-10'
        }
      }));
    });

    it('denies conversion metadata with effectiveDate after transactionDate', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_conv_future_date`), {
        transactionId: 'tx_conv_future_date',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amount: 100,
        currency: 'EUR',
        transactionDate: '2026-08-10',
        createdByUid: MEMBER_UID,
        exchangeRateMetadata: {
          source: 'BNR_OFFICIAL',
          status: 'OFFICIAL',
          rate: 4.9750,
          effectiveDate: '2026-08-15' // After 2026-08-10
        }
      }));
    });

    it('allows a transaction when exchangeRateMetadata is absent', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_conv_absent`), {
        transactionId: 'tx_conv_absent',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amount: 100,
        currency: 'RON',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('denies a transaction when exchangeRateMetadata is explicitly null', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_conv_null`), {
        transactionId: 'tx_conv_null',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amount: 100,
        currency: 'EUR',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID,
        exchangeRateMetadata: null
      }));
    });
  });

  // --------------------------------------------------------------------------
  // 5. Immutability & Tombstone / Deletion Rules
  // --------------------------------------------------------------------------
  describe('Immutability & Tombstone / Deletion Rules', () => {
    it('allows transaction update preserving immutable IDs', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(updateDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_existing`), {
        amount: 180.0
      }));
    });

    it('denies transaction update changing transactionId', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(updateDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_existing`), {
        transactionId: 'tx_altered_id'
      }));
    });

    it('denies transaction update changing householdId', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(updateDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_existing`), {
        householdId: 'household_altered'
      }));
    });

    it('denies transaction update changing createdByUid', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(updateDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_existing`), {
        createdByUid: STRANGER_UID
      }));
    });

    it('allows soft-deletion (tombstone) update by active member', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(updateDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_existing`), {
        isDeleted: true,
        deletedByUid: MEMBER_UID,
        deletedAt: '2026-08-12T01:00:00Z'
      }));
    });

    it('allows creator or owner/admin to hard-delete transaction', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(deleteDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_existing`)));
    });

    it('denies non-member from deleting transaction', async () => {
      const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
      await assertFails(deleteDoc(doc(strangerDb, `households/${HOUSEHOLD_ID}/transactions/tx_existing`)));
    });
  });

  // --------------------------------------------------------------------------
  // 6. Categories & Exchange Rates Subcollections
  // --------------------------------------------------------------------------
  describe('Categories & Exchange Rates Subcollections', () => {
    it('allows active member to create category with valid type', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/categories/cat_groceries`), {
        categoryId: 'cat_groceries',
        householdId: HOUSEHOLD_ID,
        name: 'Groceries',
        type: 'Expense',
        icon: 'ic_shopping'
      }));
    });

    it('denies category creation with invalid type', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/categories/cat_bad`), {
        categoryId: 'cat_bad',
        householdId: HOUSEHOLD_ID,
        name: 'Bad Category',
        type: 'InvalidType'
      }));
    });

    it('allows active member to create official BNR exchangeRate document', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/exchangeRates/2026-08-10`), {
        source: 'BNR_OFFICIAL',
        status: 'OFFICIAL',
        effectiveDate: '2026-08-10',
        rates: { EUR: 4.9750, USD: 4.5500 }
      }));
    });

    it('denies creation of exchangeRate document with non-official source', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/exchangeRates/2026-08-10`), {
        source: 'RANDOM_WEB',
        status: 'OFFICIAL',
        effectiveDate: '2026-08-10',
        rates: { EUR: 5.0000 }
      }));
    });
  });

  // --------------------------------------------------------------------------
  // 7. Controlled One-Time Migration Security Contract Tests
  // --------------------------------------------------------------------------
  describe('Controlled Migration Security Contract', () => {
    const MIGRATION_ID = 'mig_001';

    beforeEach(async () => {
      // Seed an active migration session initiated by OWNER_UID
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await setDoc(doc(db, `households/${HOUSEHOLD_ID}/migrationState/${MIGRATION_ID}`), {
          migrationId: MIGRATION_ID,
          householdId: HOUSEHOLD_ID,
          initiatedByUid: OWNER_UID,
          stage: 'TRANSACTIONS_UPLOADING',
          createdAt: '2026-08-13T00:00:00Z',
          updatedAt: '2026-08-13T00:00:00Z'
        });
      });
    });

    it('denies unauthenticated migration access', async () => {
      const unauthDb = testEnv.unauthenticatedContext().firestore();
      await assertFails(setDoc(doc(unauthDb, `households/${HOUSEHOLD_ID}/migrationState/mig_unauth`), {
        migrationId: 'mig_unauth',
        householdId: HOUSEHOLD_ID,
        initiatedByUid: 'anonymous',
        stage: 'PREFLIGHT'
      }));
    });

    it('denies non-member migration access', async () => {
      const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
      await assertFails(setDoc(doc(strangerDb, `households/${HOUSEHOLD_ID}/migrationState/mig_stranger`), {
        migrationId: 'mig_stranger',
        householdId: HOUSEHOLD_ID,
        initiatedByUid: STRANGER_UID,
        stage: 'PREFLIGHT'
      }));
    });

    it('denies ordinary member migration access', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/migrationState/mig_member`), {
        migrationId: 'mig_member',
        householdId: HOUSEHOLD_ID,
        initiatedByUid: MEMBER_UID,
        stage: 'PREFLIGHT'
      }));
    });

    it('allows owner or admin to create a valid migration-state document', async () => {
      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
      await assertSucceeds(setDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/migrationState/mig_owner_new`), {
        migrationId: 'mig_owner_new',
        householdId: HOUSEHOLD_ID,
        initiatedByUid: OWNER_UID,
        stage: 'PREFLIGHT'
      }));

      const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
      await assertSucceeds(setDoc(doc(adminDb, `households/${HOUSEHOLD_ID}/migrationState/mig_admin_new`), {
        migrationId: 'mig_admin_new',
        householdId: HOUSEHOLD_ID,
        initiatedByUid: ADMIN_UID,
        stage: 'PREFLIGHT'
      }));
    });

    it('allows migration write with a valid active session and preserved original createdByUid', async () => {
      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
      await assertSucceeds(setDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/transactions/tx_mig_01`), {
        transactionId: 'tx_mig_01',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amount: 250.0,
        currency: 'RON',
        transactionDate: '2026-08-01',
        createdByUid: 'user_legacy_author',
        migrationId: MIGRATION_ID
      }));
    });

    it('denies migration write without a valid active session', async () => {
      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
      await assertFails(setDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/transactions/tx_mig_no_session`), {
        transactionId: 'tx_mig_no_session',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amount: 250.0,
        currency: 'RON',
        transactionDate: '2026-08-01',
        createdByUid: 'user_legacy_author',
        migrationId: 'mig_nonexistent'
      }));
    });

    it('denies migration write for another household', async () => {
      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
      await assertFails(setDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/transactions/tx_mig_bad_h`), {
        transactionId: 'tx_mig_bad_h',
        householdId: 'household_other',
        type: 'Expense',
        account: 'Card',
        destination: null,
        amount: 250.0,
        currency: 'RON',
        transactionDate: '2026-08-01',
        createdByUid: 'user_legacy_author',
        migrationId: MIGRATION_ID
      }));
    });

    it('denies migration write initiated by another caller than the session owner', async () => {
      const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
      await assertFails(setDoc(doc(adminDb, `households/${HOUSEHOLD_ID}/transactions/tx_mig_wrong_caller`), {
        transactionId: 'tx_mig_wrong_caller',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amount: 250.0,
        currency: 'RON',
        transactionDate: '2026-08-01',
        createdByUid: ADMIN_UID,
        migrationId: MIGRATION_ID // initiated by OWNER_UID
      }));
    });

    it('denies overwriting an existing document during migration (create-only)', async () => {
      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
      await assertFails(setDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/transactions/tx_existing`), {
        transactionId: 'tx_existing',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amount: 999.0,
        currency: 'RON',
        transactionDate: '2026-08-10',
        createdByUid: MEMBER_UID,
        migrationId: MIGRATION_ID
      }));
    });

    it('denies migration write with malformed financial metadata', async () => {
      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
      await assertFails(setDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/transactions/tx_mig_malformed`), {
        transactionId: 'tx_mig_malformed',
        householdId: HOUSEHOLD_ID,
        type: 'InvalidType',
        account: 'Card',
        destination: null,
        amount: 100.0,
        currency: 'RON',
        transactionDate: '2026-08-01',
        createdByUid: OWNER_UID,
        migrationId: MIGRATION_ID
      }));
    });

    it('denies upgrading questionable metadata to OFFICIAL status', async () => {
      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
      await assertFails(setDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/exchangeRates/2026-08-01`), {
        source: 'MANUAL_IMPORT',
        status: 'OFFICIAL',
        effectiveDate: '2026-08-01',
        rates: { EUR: 4.95 },
        migrationId: MIGRATION_ID
      }));
    });

    it('allows valid PENDING, FAILED, or UNVERIFIED exchange rates under strict non-official rules', async () => {
      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
      await assertSucceeds(setDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/exchangeRates/2026-08-02`), {
        source: 'MANUAL_IMPORT',
        status: 'UNVERIFIED',
        effectiveDate: '2026-08-02',
        rates: { EUR: 4.95 },
        migrationId: MIGRATION_ID
      }));

      await assertSucceeds(setDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/exchangeRates/2026-08-03`), {
        source: 'MANUAL_IMPORT',
        status: 'PENDING',
        effectiveDate: '2026-08-03',
        rates: { EUR: 4.96 },
        migrationId: MIGRATION_ID
      }));
    });
  });
});
