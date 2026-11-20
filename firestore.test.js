const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_rep_01";
const BOB_UID = "bob_rep_02";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validCustomerPayload(uid) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    companyName: "Apex Skyline Towers",
    contactPerson: "Tariq Al-Mansoor",
    phone: "+1-555-0148",
    email: "facilities@apexskyline.com",
    city: "Chicago",
    address: "420 Wacker Drive, Suite 1800",
    segment: "COMMERCIAL",
    notes: "Key regional VRF retrofit account",
    createdAt: now,
    updatedAt: now,
  };
}

function validDealPayload(uid, customerId) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    customerId,
    customerName: "Apex Skyline Towers",
    productName: "National VRF Pro 16HP",
    productCategory: "VRF_SYSTEM",
    tonnage: 14.0,
    quantity: 4,
    unitPrice: 8500.0,
    totalValue: 34000.0,
    stage: "QUOTED",
    installationIncluded: true,
    notes: "Includes rooftop crane rigging",
    createdAt: now,
    updatedAt: now,
  };
}

function validServiceJobPayload(uid, customerId) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    customerId,
    customerName: "Apex Skyline Towers",
    serviceType: "DUCT_WORK",
    title: "East Wing GI Ductwork & Acoustic Lining",
    technicianName: "Marcus Vance",
    scheduledDate: "2026-11-25 09:00",
    city: "Chicago",
    ductSpecs: "24G Galvanized Steel, 60m run, 25mm fiberglass liner",
    estimatedCost: 6200.0,
    priority: "URGENT",
    status: "SCHEDULED",
    notes: "Access via service freight elevator",
    createdAt: now,
    updatedAt: now,
  };
}

function validAmcPayload(uid, customerId) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    customerId,
    customerName: "Apex Skyline Towers",
    contractNumber: "AMC-2026-0101",
    planTier: "COMPREHENSIVE",
    unitsCovered: 18,
    totalTonnage: 84.0,
    visitsPerYear: 4,
    visitsCompleted: 1,
    annualFee: 12400.0,
    startDate: "2026-01-01",
    endDate: "2026-12-31",
    status: "ACTIVE",
    notes: "Includes quarterly coil chemical wash and refrigerant top-up",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated users are denied across all CRM collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection(`users/${ALICE_UID}/customers`).get());
  await assertFails(unauth.collection(`users/${ALICE_UID}/deals`).get());
  await assertFails(unauth.collection(`users/${ALICE_UID}/service_jobs`).get());
  await assertFails(unauth.collection(`users/${ALICE_UID}/amc_contracts`).get());
  await assertFails(
    unauth.collection(`users/${ALICE_UID}/customers`).doc("cust_1").set(validCustomerPayload(ALICE_UID))
  );
});

test("Customers: owner create, list, partial update succeed; @DocumentId & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1").set(validCustomerPayload(ALICE_UID))
  );

  // List subcollection
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/customers`).get());

  // Partial update preserving createdAt and userId
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1").update({
      phone: "+1-555-9999",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read & write rejected
  await assertFails(bobDb.collection(`users/${ALICE_UID}/customers`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1").get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1").update({
      phone: "+1-555-0000",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / @DocumentId id field
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_2").set({
      ...validCustomerPayload(ALICE_UID),
      id: "cust_2",
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_3").set({
      ...validCustomerPayload(ALICE_UID),
      createdAt: new Date(Date.now() + 60000),
    })
  );
});

test("AC Deals: requires existing customer, supports stage progression, locks terminal WON/LOST", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Fails if parent customer does not exist
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/deals`).doc("deal_orphan").set(validDealPayload(ALICE_UID, "missing_cust"))
  );

  // Create customer first
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1").set(validCustomerPayload(ALICE_UID))
  );

  // Create valid deal
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/deals`).doc("deal_1").set(validDealPayload(ALICE_UID, "cust_1"))
  );

  // List deals
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/deals`).get());

  // Advance stage to WON
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/deals`).doc("deal_1").update({
      stage: "WON",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state locking: cannot modify after WON
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/deals`).doc("deal_1").update({
      stage: "LEAD",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Service Jobs (Maintenance/Install/Ductwork): create, status update, and terminal COMPLETED lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1").set(validCustomerPayload(ALICE_UID))
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/service_jobs`).doc("job_1").set(validServiceJobPayload(ALICE_UID, "cust_1"))
  );

  // Move to IN_PROGRESS then COMPLETED
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/service_jobs`).doc("job_1").update({
      status: "IN_PROGRESS",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/service_jobs`).doc("job_1").update({
      status: "COMPLETED",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Locked after COMPLETED
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/service_jobs`).doc("job_1").update({
      status: "SCHEDULED",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("AMC Contracts: create, log PM visit, reject visits > visitsPerYear, and lock EXPIRED", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1").set(validCustomerPayload(ALICE_UID))
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/amc_contracts`).doc("amc_1").set(validAmcPayload(ALICE_UID, "cust_1"))
  );

  // Valid PM visit increment
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/amc_contracts`).doc("amc_1").update({
      visitsCompleted: 2,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Invalid PM visits exceeding visitsPerYear (4)
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/amc_contracts`).doc("amc_1").update({
      visitsCompleted: 5,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Transition to EXPIRED locks contract
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/amc_contracts`).doc("amc_1").update({
      planTier: "COMPREHENSIVE",
      unitsCovered: 18,
      totalTonnage: 84.0,
      visitsPerYear: 4,
      visitsCompleted: 4,
      annualFee: 12400.0,
      startDate: "2026-01-01",
      endDate: "2026-12-31",
      status: "EXPIRED",
      notes: "Completed term",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/amc_contracts`).doc("amc_1").update({
      visitsCompleted: 1,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});
