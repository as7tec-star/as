const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user cannot access transactions, employees, or payrolls", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/transactions`).get()
  );
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/employees`).get()
  );
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/payrolls`).get()
  );
});

test("Cross-user access is denied on transactions, employees, and payrolls", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/transactions`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/employees`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/payrolls`).get());
});

test("Owner can create and update valid BusinessTransaction", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/transactions`)
      .doc("tx_1")
      .set({
        userId: ALICE_UID,
        type: "مبيعات",
        invoiceNumber: "INV-1001",
        partyName: "مؤسسة الأفق",
        partyPhone: "0501234567",
        partyTaxNumber: "300012345600003",
        category: "بضاعة",
        date: "2026-11-17",
        paymentMethod: "تحويل بنكي",
        paymentStatus: "مدفوع",
        taxMode: "غير شامل الضريبة",
        taxRate: 15.0,
        items: [{ name: "منتج 1", quantity: 2, unitPrice: 100.0, total: 230.0 }],
        subtotal: 200.0,
        discountTotal: 0.0,
        taxAmount: 30.0,
        grandTotal: 230.0,
        notes: "تم التسليم",
        createdAt: now,
        updatedAt: now,
      })
  );

  // Invalid type must fail
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/transactions`)
      .doc("tx_bad")
      .set({
        userId: ALICE_UID,
        type: "invalid_type",
        invoiceNumber: "INV-1002",
        partyName: "عميل",
        paymentStatus: "مدفوع",
        taxMode: "شامل الضريبة",
      })
  );
});

test("Owner can create valid Employee and PayrollRecord", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/employees`)
      .doc("emp_1")
      .set({
        userId: ALICE_UID,
        name: "سعد العتيبي",
        jobTitle: "مسؤول مبيعات",
        department: "المبيعات",
        phone: "0551112233",
        email: "saad@example.com",
        nationalId: "1012345678",
        hireDate: "2025-01-10",
        baseSalary: 5000.0,
        housingAllowance: 1250.0,
        transportAllowance: 500.0,
        otherAllowances: 0.0,
        defaultDeductions: 450.0,
        bankIban: "SA0000000000000000000000",
        status: "نشط",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/payrolls`)
      .doc("pay_1")
      .set({
        userId: ALICE_UID,
        employeeId: "emp_1",
        employeeName: "سعد العتيبي",
        jobTitle: "مسؤول مبيعات",
        employeePhone: "0551112233",
        monthYear: "2026-11",
        baseSalary: 5000.0,
        allowances: 1750.0,
        bonuses: 500.0,
        deductions: 450.0,
        netSalary: 6800.0,
        paymentStatus: "مدفوع",
        paymentDate: "2026-11-17",
        notes: "راتب شهر نوفمبر",
        createdAt: now,
        updatedAt: now,
      })
  );

  // Negative salary must fail
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/employees`)
      .doc("emp_bad")
      .set({
        userId: ALICE_UID,
        name: "اختبار",
        jobTitle: "موظف",
        baseSalary: -100.0,
        status: "نشط",
      })
  );
});

test("Products inventory rules enforce ownership and non-negative stock/thresholds", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection(`users/${ALICE_UID}/products`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/products`).get());

  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/products`)
      .doc("prod_1")
      .set({
        userId: ALICE_UID,
        name: "سماعة لاسلكية برو",
        sku: "SKU-101",
        category: "إلكترونيات",
        unit: "حبة",
        quantity: 3.0,
        minThreshold: 5.0,
        unitCost: 85.0,
        sellingPrice: 149.0,
        supplierName: "مورد التقنية",
        supplierPhone: "0501234567",
        alertsEnabled: true,
        notes: "",
        createdAt: now,
        updatedAt: now,
      })
  );

  // Partial quantity update succeeds
  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/products`)
      .doc("prod_1")
      .update({
        quantity: 2.0,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Negative quantity must fail
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/products`)
      .doc("prod_bad")
      .set({
        userId: ALICE_UID,
        name: "منتج غير صالح",
        quantity: -2.0,
        minThreshold: 5.0,
      })
  );
});
