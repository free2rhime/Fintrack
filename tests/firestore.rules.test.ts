import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
  RulesTestEnvironment
} from '@firebase/rules-unit-testing';
import { readFileSync } from 'fs';
import { doc, getDoc, setDoc, updateDoc, deleteDoc, collectionGroup, query, where, getDocs } from 'firebase/firestore';

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
        uid: OWNER_UID,
        role: 'owner',
        status: 'ACTIVE',
        joinedAt: '2026-08-01T00:00:00Z'
      });

      // Admin member
      await setDoc(doc(db, `households/${HOUSEHOLD_ID}/members/${ADMIN_UID}`), {
        uid: ADMIN_UID,
        role: 'admin',
        status: 'ACTIVE',
        joinedAt: '2026-08-01T00:00:00Z'
      });

      // Regular member
      await setDoc(doc(db, `households/${HOUSEHOLD_ID}/members/${MEMBER_UID}`), {
        uid: MEMBER_UID,
        role: 'member',
        status: 'ACTIVE',
        joinedAt: '2026-08-01T00:00:00Z'
      });

      // Inactive member
      await setDoc(doc(db, `households/${HOUSEHOLD_ID}/members/${INACTIVE_UID}`), {
        uid: INACTIVE_UID,
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
        amountRon: 150.0,
        amountEur: 30.0,
        exchangeRate: 5.0,
        description: 'Existing groceries',
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
        amountRon: 150.0,
        amountEur: 30.0,
        exchangeRate: 5.0,
        description: 'Unauth tx',
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
  // 2. Membership Administration & Self-Join Prevention & Role Security
  // --------------------------------------------------------------------------
  describe('Membership Administration & Self-Join Prevention & Role Security', () => {
    it('denies self-join: stranger writing member doc for themselves without inviteId', async () => {
      const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
      await assertFails(setDoc(doc(strangerDb, `households/${HOUSEHOLD_ID}/members/${STRANGER_UID}`), {
        role: 'member',
        status: 'ACTIVE',
        joinedAt: 1770001000000
      }));
    });

    it('denies self-join: stranger supplying valid ownerUid as invitedByUid but missing inviteId', async () => {
      const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
      await assertFails(setDoc(doc(strangerDb, `households/${HOUSEHOLD_ID}/members/${STRANGER_UID}`), {
        uid: STRANGER_UID,
        role: 'member',
        status: 'ACTIVE',
        joinedAt: 1770001000000,
        invitedByUid: OWNER_UID
      }));
    });

    it('denies self-join: stranger supplying forged or non-existent inviteId', async () => {
      const strangerDb = testEnv.authenticatedContext(STRANGER_UID, { email: 'stranger@fintrack.test' }).firestore();
      await assertFails(setDoc(doc(strangerDb, `households/${HOUSEHOLD_ID}/members/${STRANGER_UID}`), {
        uid: STRANGER_UID,
        email: 'stranger@fintrack.test',
        role: 'member',
        status: 'ACTIVE',
        joinedAt: 1770001000000,
        invitedByUid: OWNER_UID,
        inviteId: 'non_existent_invite_id'
      }));
    });

    it('denies regular member adding a new member', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/members/user_new`), {
        role: 'member',
        status: 'ACTIVE',
        joinedAt: 1770001000000
      }));
    });

    it('denies regular member updating another member role', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(updateDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/members/${ADMIN_UID}`), {
        role: 'member'
      }));
    });

    it('denies regular member changing own role', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(updateDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/members/${MEMBER_UID}`), {
        role: 'owner'
      }));
    });

    it('denies regular member changing own status', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(updateDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/members/${MEMBER_UID}`), {
        status: 'INACTIVE'
      }));
    });

    it('denies admin escalating own role to owner', async () => {
      const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
      await assertFails(updateDoc(doc(adminDb, `households/${HOUSEHOLD_ID}/members/${ADMIN_UID}`), {
        role: 'owner'
      }));
    });

    it('denies admin deleting the owner member document', async () => {
      const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
      await assertFails(deleteDoc(doc(adminDb, `households/${HOUSEHOLD_ID}/members/${OWNER_UID}`)));
    });

    it('allows household owner to change member role', async () => {
      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
      await assertSucceeds(updateDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/members/${MEMBER_UID}`), {
        role: 'admin'
      }));
    });

    it('allows household owner to delete member', async () => {
      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
      await assertSucceeds(deleteDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/members/${MEMBER_UID}`)));
    });

    it('denies household owner deleting own member document', async () => {
      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
      await assertFails(deleteDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/members/${OWNER_UID}`)));
    });

    it('allows household owner or admin to add a new member directly', async () => {
      const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
      await assertSucceeds(setDoc(doc(adminDb, `households/${HOUSEHOLD_ID}/members/user_new`), {
        role: 'member',
        status: 'ACTIVE',
        joinedAt: 1770001000000
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
        joinedAt: 1770001000000
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
        amountRon: 5000.0,
        amountEur: 1000.0,
        exchangeRate: 5.0,
        description: 'Salary Bubu',
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
        amountRon: 3000.0,
        amountEur: 600.0,
        exchangeRate: 5.0,
        description: 'Salary Piticania',
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
        amountRon: 700.0,
        amountEur: 140.0,
        exchangeRate: 5.0,
        description: 'Meal ticket bonus',
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
        amountRon: 1000.0,
        amountEur: 200.0,
        exchangeRate: 5.0,
        description: 'Invalid dest',
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
        amountRon: 120.0,
        amountEur: 24.0,
        exchangeRate: 5.0,
        description: 'Dinner with family',
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
        amountRon: 120.0,
        amountEur: 24.0,
        exchangeRate: 5.0,
        description: 'Expense with dest',
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
        amountRon: 200.0,
        amountEur: 40.0,
        exchangeRate: 5.0,
        description: 'Bad type',
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
        amountRon: 200.0,
        amountEur: 40.0,
        exchangeRate: 5.0,
        description: 'Bad account',
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
        amountRon: 45.0,
        amountEur: 9.0,
        exchangeRate: 5.0,
        description: 'Lunch',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('denies transaction with negative amountRon', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_neg_ron`), {
        transactionId: 'tx_neg_ron',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amountRon: -50.0,
        amountEur: 0.0,
        exchangeRate: 5.0,
        description: 'Negative RON',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('denies transaction with zero amountRon', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_zero_ron`), {
        transactionId: 'tx_zero_ron',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amountRon: 0.0,
        amountEur: 0.0,
        exchangeRate: 5.0,
        description: 'Zero RON',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('denies transaction with string amountRon', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_str_ron`), {
        transactionId: 'tx_str_ron',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amountRon: '100.0',
        amountEur: 20.0,
        exchangeRate: 5.0,
        description: 'String RON',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('denies transaction with negative amountEur', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_neg_eur`), {
        transactionId: 'tx_neg_eur',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amountRon: 100.0,
        amountEur: -20.0,
        exchangeRate: 5.0,
        description: 'Negative EUR',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('denies transaction with string amountEur', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_str_eur`), {
        transactionId: 'tx_str_eur',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amountRon: 100.0,
        amountEur: '20.0',
        exchangeRate: 5.0,
        description: 'String EUR',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('denies transaction with negative exchangeRate', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_neg_rate`), {
        transactionId: 'tx_neg_rate',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amountRon: 100.0,
        amountEur: 20.0,
        exchangeRate: -5.0,
        description: 'Negative rate',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('denies transaction with string exchangeRate', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_str_rate`), {
        transactionId: 'tx_str_rate',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amountRon: 100.0,
        amountEur: 20.0,
        exchangeRate: '5.0',
        description: 'String rate',
        transactionDate: '2026-08-12',
        createdByUid: MEMBER_UID
      }));
    });

    it('denies transaction with missing transactionDate', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_no_date`), {
        transactionId: 'tx_no_date',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amountRon: 100.0,
        amountEur: 20.0,
        exchangeRate: 5.0,
        description: 'Missing date',
        createdByUid: MEMBER_UID
      }));
    });

    it('denies transaction with missing description', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_no_desc`), {
        transactionId: 'tx_no_desc',
        householdId: HOUSEHOLD_ID,
        type: 'Expense',
        account: 'Card',
        destination: null,
        amountRon: 100.0,
        amountEur: 20.0,
        exchangeRate: 5.0,
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
        amountRon: 497.50,
        amountEur: 100.0,
        exchangeRate: 4.9750,
        description: 'EUR Purchase',
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
        amountRon: 497.50,
        amountEur: 100.0,
        exchangeRate: 4.9750,
        description: 'Unofficial source',
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
        amountRon: 497.50,
        amountEur: 100.0,
        exchangeRate: 4.9750,
        description: 'Negative conversion rate',
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
        amountRon: 497.50,
        amountEur: 100.0,
        exchangeRate: 4.9750,
        description: 'Future date metadata',
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
  });

  // --------------------------------------------------------------------------
  // 5. Immutability & Tombstone / Deletion Rules
  // --------------------------------------------------------------------------
  describe('Immutability & Tombstone / Deletion Rules', () => {
    it('allows transaction update preserving immutable IDs', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(updateDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/transactions/tx_existing`), {
        amountRon: 180.0,
        amountEur: 36.0,
        exchangeRate: 5.0,
        description: 'Updated groceries'
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
    const CAT_EXISTING_ID = 'cat_existing_food';

    beforeEach(async () => {
      // Seed a pre-existing category in household_100
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await setDoc(doc(db, `households/${HOUSEHOLD_ID}/categories/${CAT_EXISTING_ID}`), {
          categoryId: CAT_EXISTING_ID,
          householdId: HOUSEHOLD_ID,
          name: 'Food & Dining',
          type: 'Expense',
          subCategory: 'Restaurants'
        });

        // Seed a category in a different household for isolation tests
        await setDoc(doc(db, 'households/household_other/categories/cat_other_hh'), {
          categoryId: 'cat_other_hh',
          householdId: 'household_other',
          name: 'Other HH Category',
          type: 'Expense'
        });
      });
    });

    // A. UNAUTHENTICATED
    it('denies unauthenticated read, create, update, delete on categories', async () => {
      const unauthDb = testEnv.unauthenticatedContext().firestore();

      // Read
      await assertFails(getDoc(doc(unauthDb, `households/${HOUSEHOLD_ID}/categories/${CAT_EXISTING_ID}`)));

      // Create
      await assertFails(setDoc(doc(unauthDb, `households/${HOUSEHOLD_ID}/categories/cat_unauth`), {
        categoryId: 'cat_unauth',
        householdId: HOUSEHOLD_ID,
        name: 'Unauth Category',
        type: 'Expense'
      }));

      // Update
      await assertFails(updateDoc(doc(unauthDb, `households/${HOUSEHOLD_ID}/categories/${CAT_EXISTING_ID}`), {
        name: 'Hacked Name'
      }));

      // Delete
      await assertFails(deleteDoc(doc(unauthDb, `households/${HOUSEHOLD_ID}/categories/${CAT_EXISTING_ID}`)));
    });

    // B. MEMBER (Read ALLOW, Create/Update/Delete DENY)
    it('allows active member to read categories', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertSucceeds(getDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/categories/${CAT_EXISTING_ID}`)));
    });

    it('denies active member from creating a category', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/categories/cat_member_new`), {
        categoryId: 'cat_member_new',
        householdId: HOUSEHOLD_ID,
        name: 'Groceries',
        type: 'Expense',
        icon: 'ic_shopping'
      }));
    });

    it('denies active member from updating a category', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(updateDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/categories/${CAT_EXISTING_ID}`), {
        name: 'Food & Groceries',
        type: 'Expense',
        householdId: HOUSEHOLD_ID
      }));
    });

    it('denies active member from deleting a category', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(deleteDoc(doc(memberDb, `households/${HOUSEHOLD_ID}/categories/${CAT_EXISTING_ID}`)));
    });

    // C. ADMIN (Full CRUD ALLOW)
    it('allows admin to read, create, update, and delete categories', async () => {
      const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

      // Read
      await assertSucceeds(getDoc(doc(adminDb, `households/${HOUSEHOLD_ID}/categories/${CAT_EXISTING_ID}`)));

      // Create
      const newCatId = 'cat_admin_created';
      await assertSucceeds(setDoc(doc(adminDb, `households/${HOUSEHOLD_ID}/categories/${newCatId}`), {
        categoryId: newCatId,
        householdId: HOUSEHOLD_ID,
        name: 'Utilities',
        type: 'Expense'
      }));

      // Update
      await assertSucceeds(updateDoc(doc(adminDb, `households/${HOUSEHOLD_ID}/categories/${newCatId}`), {
        name: 'Utilities & Bills',
        type: 'Expense',
        householdId: HOUSEHOLD_ID
      }));

      // Delete
      await assertSucceeds(deleteDoc(doc(adminDb, `households/${HOUSEHOLD_ID}/categories/${newCatId}`)));
    });

    // D. OWNER (Full CRUD ALLOW)
    it('allows owner full CRUD permissions on categories', async () => {
      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();

      // Read
      await assertSucceeds(getDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/categories/${CAT_EXISTING_ID}`)));

      // Create
      const ownerCatId = 'cat_owner_created';
      await assertSucceeds(setDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/categories/${ownerCatId}`), {
        categoryId: ownerCatId,
        householdId: HOUSEHOLD_ID,
        name: 'Salary',
        type: 'Income'
      }));

      // Update
      await assertSucceeds(updateDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/categories/${ownerCatId}`), {
        name: 'Primary Salary',
        type: 'Income',
        householdId: HOUSEHOLD_ID
      }));

      // Delete
      await assertSucceeds(deleteDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/categories/${ownerCatId}`)));
    });

    // E. CROSS-HOUSEHOLD ISOLATION
    it('denies cross-household category read and write', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

      // Read other household category
      await assertFails(getDoc(doc(memberDb, 'households/household_other/categories/cat_other_hh')));

      // Create in other household
      await assertFails(setDoc(doc(adminDb, 'households/household_other/categories/cat_cross_write'), {
        categoryId: 'cat_cross_write',
        householdId: 'household_other',
        name: 'Cross Write',
        type: 'Expense'
      }));
    });

    // F. householdId INTEGRITY
    it('denies category creation when householdId does not match path', async () => {
      const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
      await assertFails(setDoc(doc(adminDb, `households/${HOUSEHOLD_ID}/categories/cat_mismatched`), {
        categoryId: 'cat_mismatched',
        householdId: 'different_household_id',
        name: 'Mismatched HH',
        type: 'Expense'
      }));
    });

    it('denies category update attempting to mutate householdId', async () => {
      const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
      await assertFails(updateDoc(doc(adminDb, `households/${HOUSEHOLD_ID}/categories/${CAT_EXISTING_ID}`), {
        householdId: 'tampered_household_id'
      }));
    });

    // G. TYPE VALIDATION
    it('denies category creation and update with invalid transaction type', async () => {
      const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

      // Invalid type on create
      await assertFails(setDoc(doc(adminDb, `households/${HOUSEHOLD_ID}/categories/cat_bad_type`), {
        categoryId: 'cat_bad_type',
        householdId: HOUSEHOLD_ID,
        name: 'Bad Category',
        type: 'InvalidType'
      }));

      // Invalid type on update
      await assertFails(updateDoc(doc(adminDb, `households/${HOUSEHOLD_ID}/categories/${CAT_EXISTING_ID}`), {
        type: 'BadType',
        householdId: HOUSEHOLD_ID
      }));
    });

    // H. MIGRATION SESSION CATEGORY WRITE
    it('allows category creation during active migration session in valid stage', async () => {
      const MIGRATION_ID = 'mig_cat_test_001';

      // Seed active migration session
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await setDoc(doc(db, `households/${HOUSEHOLD_ID}/migrationState/${MIGRATION_ID}`), {
          migrationId: MIGRATION_ID,
          householdId: HOUSEHOLD_ID,
          initiatedByUid: OWNER_UID,
          stage: 'CATEGORIES_UPLOADING',
          totalTransactions: 0,
          uploadedTransactions: 0,
          totalCategories: 5,
          uploadedCategories: 0,
          totalRates: 0,
          uploadedRates: 0,
          startedAt: '2026-08-10T12:00:00Z',
          completedAt: null,
          failureReason: null
        });
      });

      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
      await assertSucceeds(setDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/categories/cat_migrated`), {
        categoryId: 'cat_migrated',
        householdId: HOUSEHOLD_ID,
        name: 'Migrated Category',
        type: 'Expense',
        migrationId: MIGRATION_ID
      }));
    });

    it('denies category creation when migrationId references completed or failed session', async () => {
      const MIGRATION_ID_COMPLETED = 'mig_cat_completed';

      // Seed completed migration session
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await setDoc(doc(db, `households/${HOUSEHOLD_ID}/migrationState/${MIGRATION_ID_COMPLETED}`), {
          migrationId: MIGRATION_ID_COMPLETED,
          householdId: HOUSEHOLD_ID,
          initiatedByUid: OWNER_UID,
          stage: 'COMPLETED',
          totalTransactions: 0,
          uploadedTransactions: 0,
          totalCategories: 0,
          uploadedCategories: 0,
          totalRates: 0,
          uploadedRates: 0,
          startedAt: '2026-08-10T12:00:00Z',
          completedAt: '2026-08-10T12:05:00Z',
          failureReason: null
        });
      });

      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
      await assertFails(setDoc(doc(ownerDb, `households/${HOUSEHOLD_ID}/categories/cat_mig_failed`), {
        categoryId: 'cat_mig_failed',
        householdId: HOUSEHOLD_ID,
        name: 'Late Migrated Category',
        type: 'Expense',
        migrationId: MIGRATION_ID_COMPLETED
      }));
    });

    // Exchange Rates Subcollection Tests
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
        amountRon: 250.0,
        amountEur: 50.0,
        exchangeRate: 5.0,
        description: 'Migrated tx',
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
        amountRon: 250.0,
        amountEur: 50.0,
        exchangeRate: 5.0,
        description: 'Migrated tx',
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
        amountRon: 250.0,
        amountEur: 50.0,
        exchangeRate: 5.0,
        description: 'Migrated tx',
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
        amountRon: 250.0,
        amountEur: 50.0,
        exchangeRate: 5.0,
        description: 'Migrated tx',
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
        amountRon: 999.0,
        amountEur: 199.8,
        exchangeRate: 5.0,
        description: 'Migrated overwrite attempt',
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
        amountRon: 100.0,
        amountEur: 20.0,
        exchangeRate: 5.0,
        description: 'Malformed type',
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

  // --------------------------------------------------------------------------
  // 8. Invitation Rules & Membership Invitation Flow
  // --------------------------------------------------------------------------
  describe('Invitation Security Rules', () => {
    const INVITEE_EMAIL = 'invitee@fintrack.test';
    const INVITEE_UID = 'user_invitee';
    const INVITE_ID = 'invite_001';
    const INVITE_ACCEPTED_ID = 'invite_accepted_002';
    const INVITE_OTHER_HH_ID = 'invite_other_hh_003';
    const INVITE_OTHER_EMAIL_ID = 'invite_other_email_004';

    beforeEach(async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();

        // Valid PENDING invitation for INVITEE_EMAIL in HOUSEHOLD_ID
        await setDoc(doc(db, `invitations/${INVITE_ID}`), {
          inviteId: INVITE_ID,
          householdId: HOUSEHOLD_ID,
          householdName: 'The FinTrack Family',
          inviterUid: OWNER_UID,
          inviterEmail: 'owner@fintrack.test',
          inviteeEmail: INVITEE_EMAIL,
          targetRole: 'member',
          status: 'PENDING',
          createdAt: 1770000000000,
          expiresAt: 1780000000000
        });

        // Already ACCEPTED invitation (replay test)
        await setDoc(doc(db, `invitations/${INVITE_ACCEPTED_ID}`), {
          inviteId: INVITE_ACCEPTED_ID,
          householdId: HOUSEHOLD_ID,
          householdName: 'The FinTrack Family',
          inviterUid: OWNER_UID,
          inviterEmail: 'owner@fintrack.test',
          inviteeEmail: INVITEE_EMAIL,
          targetRole: 'member',
          status: 'ACCEPTED',
          createdAt: 1770000000000,
          expiresAt: 1780000000000,
          respondedAt: 1770000500000
        });

        // Invitation for a DIFFERENT household
        await setDoc(doc(db, `invitations/${INVITE_OTHER_HH_ID}`), {
          inviteId: INVITE_OTHER_HH_ID,
          householdId: 'household_different',
          householdName: 'Other Family',
          inviterUid: OWNER_UID,
          inviterEmail: 'owner@fintrack.test',
          inviteeEmail: INVITEE_EMAIL,
          targetRole: 'member',
          status: 'PENDING',
          createdAt: 1770000000000,
          expiresAt: 1780000000000
        });

        // Invitation for a DIFFERENT email address
        await setDoc(doc(db, `invitations/${INVITE_OTHER_EMAIL_ID}`), {
          inviteId: INVITE_OTHER_EMAIL_ID,
          householdId: HOUSEHOLD_ID,
          householdName: 'The FinTrack Family',
          inviterUid: OWNER_UID,
          inviterEmail: 'owner@fintrack.test',
          inviteeEmail: 'someoneelse@fintrack.test',
          targetRole: 'member',
          status: 'PENDING',
          createdAt: 1770000000000,
          expiresAt: 1780000000000
        });
      });
    });

    it('allows household owner to create invitation', async () => {
      const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
      await assertSucceeds(setDoc(doc(ownerDb, 'invitations/invite_new'), {
        inviteId: 'invite_new',
        householdId: HOUSEHOLD_ID,
        householdName: 'The FinTrack Family',
        inviterUid: OWNER_UID,
        inviterEmail: 'owner@fintrack.test',
        inviteeEmail: 'newperson@fintrack.test',
        targetRole: 'member',
        status: 'PENDING',
        createdAt: 1770000000000,
        expiresAt: 1780000000000
      }));
    });

    it('denies non-owner from creating invitation', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      await assertFails(setDoc(doc(memberDb, 'invitations/invite_bad'), {
        inviteId: 'invite_bad',
        householdId: HOUSEHOLD_ID,
        householdName: 'The FinTrack Family',
        inviterUid: MEMBER_UID,
        inviterEmail: 'member@fintrack.test',
        inviteeEmail: 'newperson@fintrack.test',
        targetRole: 'member',
        status: 'PENDING',
        createdAt: 1770000000000,
        expiresAt: 1780000000000
      }));
    });

    it('allows invitee with matching email to read invitation', async () => {
      const inviteeDb = testEnv.authenticatedContext(INVITEE_UID, { email: INVITEE_EMAIL }).firestore();
      await assertSucceeds(getDoc(doc(inviteeDb, `invitations/${INVITE_ID}`)));
    });

    it('denies unrelated stranger from reading invitation', async () => {
      const strangerDb = testEnv.authenticatedContext(STRANGER_UID, { email: 'stranger@fintrack.test' }).firestore();
      await assertFails(getDoc(doc(strangerDb, `invitations/${INVITE_ID}`)));
    });

    it('allows invitee with matching email to accept invitation', async () => {
      const inviteeDb = testEnv.authenticatedContext(INVITEE_UID, { email: INVITEE_EMAIL }).firestore();
      await assertSucceeds(updateDoc(doc(inviteeDb, `invitations/${INVITE_ID}`), {
        status: 'ACCEPTED',
        respondedAt: 1770001000000
      }));
    });

    it('allows invitee with matching email to decline invitation', async () => {
      const inviteeDb = testEnv.authenticatedContext(INVITEE_UID, { email: INVITEE_EMAIL }).firestore();
      await assertSucceeds(updateDoc(doc(inviteeDb, `invitations/${INVITE_ID}`), {
        status: 'DECLINED',
        respondedAt: 1770001000000
      }));
    });

    it('allows invited member to create their active member doc with valid invitation', async () => {
      const inviteeDb = testEnv.authenticatedContext(INVITEE_UID, { email: INVITEE_EMAIL }).firestore();
      await assertSucceeds(setDoc(doc(inviteeDb, `households/${HOUSEHOLD_ID}/members/${INVITEE_UID}`), {
        uid: INVITEE_UID,
        email: INVITEE_EMAIL,
        role: 'member',
        status: 'ACTIVE',
        joinedAt: 1770001000000,
        invitedByUid: OWNER_UID,
        inviteId: INVITE_ID
      }));
    });

    it('denies member creation with already ACCEPTED invitation (replay protection)', async () => {
      const inviteeDb = testEnv.authenticatedContext(INVITEE_UID, { email: INVITEE_EMAIL }).firestore();
      await assertFails(setDoc(doc(inviteeDb, `households/${HOUSEHOLD_ID}/members/${INVITEE_UID}`), {
        uid: INVITEE_UID,
        email: INVITEE_EMAIL,
        role: 'member',
        status: 'ACTIVE',
        joinedAt: 1770001000000,
        invitedByUid: OWNER_UID,
        inviteId: INVITE_ACCEPTED_ID
      }));
    });

    it('denies member creation with invitation belonging to another household', async () => {
      const inviteeDb = testEnv.authenticatedContext(INVITEE_UID, { email: INVITEE_EMAIL }).firestore();
      await assertFails(setDoc(doc(inviteeDb, `households/${HOUSEHOLD_ID}/members/${INVITEE_UID}`), {
        uid: INVITEE_UID,
        email: INVITEE_EMAIL,
        role: 'member',
        status: 'ACTIVE',
        joinedAt: 1770001000000,
        invitedByUid: OWNER_UID,
        inviteId: INVITE_OTHER_HH_ID
      }));
    });

    it('denies member creation with invitation addressed to another email', async () => {
      const inviteeDb = testEnv.authenticatedContext(INVITEE_UID, { email: INVITEE_EMAIL }).firestore();
      await assertFails(setDoc(doc(inviteeDb, `households/${HOUSEHOLD_ID}/members/${INVITEE_UID}`), {
        uid: INVITEE_UID,
        email: INVITEE_EMAIL,
        role: 'member',
        status: 'ACTIVE',
        joinedAt: 1770001000000,
        invitedByUid: OWNER_UID,
        inviteId: INVITE_OTHER_EMAIL_ID
      }));
    });
  });

  // --------------------------------------------------------------------------
  // 9. Collection-Group Member Resolution Rules
  // --------------------------------------------------------------------------
  describe('Collection-Group Member Resolution Rules', () => {
    it('allows authenticated user to run collectionGroup("members") query for their own ACTIVE membership', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      const q = query(
        collectionGroup(memberDb, 'members'),
        where('uid', '==', MEMBER_UID),
        where('status', '==', 'ACTIVE')
      );
      await assertSucceeds(getDocs(q));
    });

    it('denies authenticated user from querying another user\'s membership via collection-group query', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      const q = query(
        collectionGroup(memberDb, 'members'),
        where('uid', '==', STRANGER_UID),
        where('status', '==', 'ACTIVE')
      );
      await assertFails(getDocs(q));
    });

    it('denies authenticated user from querying their own INACTIVE membership via collection-group query', async () => {
      const inactiveDb = testEnv.authenticatedContext(INACTIVE_UID).firestore();
      const q = query(
        collectionGroup(inactiveDb, 'members'),
        where('uid', '==', INACTIVE_UID),
        where('status', '==', 'INACTIVE')
      );
      await assertFails(getDocs(q));
    });

    it('denies unauthenticated user from executing collectionGroup("members") query', async () => {
      const unauthedDb = testEnv.unauthenticatedContext().firestore();
      const q = query(
        collectionGroup(unauthedDb, 'members'),
        where('uid', '==', MEMBER_UID),
        where('status', '==', 'ACTIVE')
      );
      await assertFails(getDocs(q));
    });

    it('denies collectionGroup("members") query missing the uid constraint', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      const q = query(
        collectionGroup(memberDb, 'members'),
        where('status', '==', 'ACTIVE')
      );
      await assertFails(getDocs(q));
    });

    it('denies collectionGroup("members") query missing the ACTIVE status constraint', async () => {
      const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
      const q = query(
        collectionGroup(memberDb, 'members'),
        where('uid', '==', MEMBER_UID)
      );
      await assertFails(getDocs(q));
    });

    it('denies write, update, or delete operations through the collection-group rule', async () => {
      const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
      // Attempting to write a member document outside authorized household paths
      await assertFails(setDoc(doc(strangerDb, `unauthorized_root/${STRANGER_UID}/members/${STRANGER_UID}`), {
        uid: STRANGER_UID,
        status: 'ACTIVE',
        role: 'member'
      }));
    });
  });
});
