const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_001";
const BOB_UID = "bob_student_002";
const COUNSELLOR_UID = "counsellor_001";
const ADMIN_EMAIL = "abhishekkhulkhule@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

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

test("Unauthenticated users: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("courses").get());
  await assertFails(unauthDb.collection("colleges").get());
  await assertFails(unauthDb.collection("packages").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("student_preferences").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("payments").get());
});

test("Student profile PII isolation & self-update bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = pastDate(5000);

  // Alice creates her own profile (without @DocumentId `id`)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      studentCode: "MP-2026-001",
      fullName: "Alice Kulkarni",
      email: "alice@example.com",
      mobile: "9876543210",
      state: "Maharashtra",
      district: "Pune",
      domicile: "Maharashtra",
      preferredCourseId: "course_btech",
      preferredCourseName: "B.Tech Engineering",
      entranceExam: "MHT-CET",
      examScore: 158.5,
      percentile: 98.42,
      rank: 2450,
      category: "OPEN",
      budgetMax: 250000,
      counsellingStatus: "NEW",
      blocked: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice cannot read Bob or list /users
  await assertFails(aliceDb.collection("users").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Alice can update her own academic fields
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      percentile: 99.1,
      updatedAt: pastDate(1000),
    })
  );

  // Alice CANNOT escalate her own blocked/counsellingStatus or mutate createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      counsellingStatus: "COMPLETED",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
});

test("Student Preference List strict privacy: isolated from other students, visible to Admin/Counsellor", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("admins").doc(COUNSELLOR_UID).set({
      uid: COUNSELLOR_UID,
      email: "counsellor@meritpath.in",
      displayName: "Dr. Deshmukh",
      role: "COUNSELLOR",
      permissions: ["VIEW_PREFERENCES", "MANAGE_COUNSELLING"],
      active: true,
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const counsellorDb = testEnv.authenticatedContext(COUNSELLOR_UID, { email: "counsellor@meritpath.in", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext("admin_owner", { email: ADMIN_EMAIL, email_verified: true }).firestore();

  const created = pastDate(8000);

  // Alice creates a DRAFT preference list
  await assertSucceeds(
    aliceDb.collection("student_preferences").doc(ALICE_UID).set({
      userId: ALICE_UID,
      studentCode: "MP-2026-001",
      studentName: "Alice Kulkarni",
      mobile: "9876543210",
      courseId: "course_btech",
      courseName: "B.Tech Engineering",
      percentile: 98.42,
      examScore: 158.5,
      rank: 2450,
      category: "OPEN",
      packageTier: "STANDARD",
      items: [
        { order: 1, collegeId: "col_coep", collegeName: "COEP Technological University", branchName: "Computer Engineering", city: "Pune" }
      ],
      status: "DRAFT",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob CANNOT get Alice's preference list or list unfiltered preferences
  await assertFails(bobDb.collection("student_preferences").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("student_preferences").get());
  await assertFails(bobDb.collection("student_preferences").where("userId", "==", ALICE_UID).get());

  // Alice can query her own preferences with whereEqualTo("userId", ALICE_UID)
  await assertSucceeds(aliceDb.collection("student_preferences").where("userId", "==", ALICE_UID).get());

  // Alice submits final list -> status transitions from DRAFT to SUBMITTED
  await assertSucceeds(
    aliceDb.collection("student_preferences").doc(ALICE_UID).update({
      status: "SUBMITTED",
      submittedAtEpochMs: Date.now() - 1000,
      updatedAt: pastDate(1000),
    })
  );

  // Once SUBMITTED, Alice cannot tamper with her submitted list directly
  await assertFails(
    aliceDb.collection("student_preferences").doc(ALICE_UID).update({
      items: [],
      updatedAt: pastDate(500),
    })
  );

  // Counsellor & Admin CAN list and review submitted preference lists
  await assertSucceeds(counsellorDb.collection("student_preferences").get());
  await assertSucceeds(adminDb.collection("student_preferences").get());
  await assertSucceeds(
    counsellorDb.collection("student_preferences").doc(ALICE_UID).update({
      counsellingStatus: "SCHEDULED",
      adminNotes: "High probability in CAP Round 1",
      updatedAt: pastDate(400),
    })
  );
});

test("Atomic payment verification and package entitlement unlocking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = pastDate(5000);

  // Direct entitlement creation WITHOUT a SUCCESS payment record MUST FAIL
  await assertFails(
    aliceDb.collection("student_packages").doc(`${ALICE_UID}_pkg_std`).set({
      userId: ALICE_UID,
      packageId: "pkg_std",
      packageName: "Standard Package",
      tier: "STANDARD",
      paymentId: "missing_pay_1",
      features: ["College Explorer", "Preference List"],
      active: true,
      activatedAtEpochMs: Date.now() - 2000,
      expiresAtEpochMs: Date.now() + 86400000,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Atomic batch write: create verified SUCCESS payment + student_packages entitlement together
  const batch = aliceDb.batch();
  const payRef = aliceDb.collection("payments").doc("pay_alice_01");
  const entRef = aliceDb.collection("student_packages").doc(`${ALICE_UID}_pkg_std`);

  batch.set(payRef, {
    userId: ALICE_UID,
    studentName: "Alice Kulkarni",
    studentEmail: "alice@example.com",
    packageId: "pkg_std",
    packageName: "Standard Package",
    packageTier: "STANDARD",
    originalAmount: 1999,
    discountAmount: 500,
    promoCode: "MERIT500",
    finalAmount: 1499,
    currency: "INR",
    gatewayOrderId: "order_rzp_001",
    gatewayPaymentId: "pay_rzp_001",
    gatewaySignature: "sig_verified_hmac",
    paymentMethod: "UPI",
    mode: "SANDBOX",
    status: "SUCCESS",
    receiptNumber: "RCPT-2026-0001",
    verifiedByBackend: true,
    createdAt: now,
    updatedAt: now,
  });

  batch.set(entRef, {
    userId: ALICE_UID,
    packageId: "pkg_std",
    packageName: "Standard Package",
    tier: "STANDARD",
    paymentId: "pay_alice_01",
    features: ["College Explorer", "Preference List"],
    active: true,
    activatedAtEpochMs: Date.now() - 2000,
    expiresAtEpochMs: Date.now() + 365 * 86400000,
    createdAt: now,
    updatedAt: now,
  });

  await assertSucceeds(batch.commit());

  // Bob cannot read Alice's payments or packages
  await assertFails(bobDb.collection("payments").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("student_packages").where("userId", "==", ALICE_UID).get());

  // Alice can query her own payments and packages
  await assertSucceeds(aliceDb.collection("payments").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("student_packages").where("userId", "==", ALICE_UID).get());
});

test("Catalog RBAC & Notification audience isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext("admin_owner", { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  const now = pastDate(3000);

  // Spoofed unverified admin email MUST fail
  await assertFails(
    spoofedAdminDb.collection("courses").doc("course_btech").set({
      name: "B.Tech",
      code: "BTECH",
      description: "Engineering",
      eligibility: "12th PCM",
      duration: "4 Years",
      entranceExam: "MHT-CET / JEE",
      active: true,
      displayOrder: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Verified Admin creates course
  await assertSucceeds(
    adminDb.collection("courses").doc("course_btech").set({
      name: "B.Tech",
      code: "BTECH",
      description: "Engineering",
      eligibility: "12th PCM",
      duration: "4 Years",
      entranceExam: "MHT-CET / JEE",
      active: true,
      displayOrder: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student can read courses, cannot write courses
  await assertSucceeds(aliceDb.collection("courses").get());
  await assertFails(aliceDb.collection("courses").doc("course_btech").delete());

  // Notifications: Admin creates global and targeted notifications
  await assertSucceeds(
    adminDb.collection("notifications").doc("notif_all").set({
      title: "CAP Round 1 Schedule",
      message: "Option form filling begins tomorrow.",
      type: "CAP_ROUND",
      targetUserId: "ALL",
      audience: "ALL",
      createdByUid: "admin_owner",
      createdAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("notifications").doc("notif_bob").set({
      title: "Bob Counselling",
      message: "Private alert for Bob.",
      type: "COUNSELLING",
      targetUserId: BOB_UID,
      audience: "STUDENT",
      createdByUid: "admin_owner",
      createdAt: now,
    })
  );

  // Alice can query whereIn("targetUserId", [ALICE_UID, "ALL"]), cannot read Bob's notification
  await assertSucceeds(
    aliceDb.collection("notifications").where("targetUserId", "in", [ALICE_UID, "ALL"]).get()
  );
  await assertFails(aliceDb.collection("notifications").doc("notif_bob").get());
  await assertFails(aliceDb.collection("notifications").get());
});
