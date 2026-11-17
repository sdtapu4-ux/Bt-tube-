const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_gmail_123";
const BOB_UID = "bob_gmail_456";

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

test("Unauthenticated user cannot read, list, or write users collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@gmail.com",
      streamOnMobileData: true,
      defaultQuality: "SD_360",
      autoplayNext: true,
      notificationsEnabled: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated Gmail owner can create profile and update SD quality and mobile data settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Gmail",
      email: "alice@gmail.com",
      photoUrl: "https://lh3.googleusercontent.com/a/avatar",
      streamOnMobileData: true,
      defaultQuality: "SD_480",
      autoplayNext: true,
      notificationsEnabled: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Update SD quality & mobile data preference (preserving immutable createdAt & userId)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      defaultQuality: "SD_144",
      streamOnMobileData: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid quality tier
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      defaultQuality: "INVALID_RES",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      defaultQuality: "SD_240",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject shadow/ghost field injection & immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user PII isolation prevents reading, listing, or updating another Gmail user's document", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      email: "bob@gmail.com",
      streamOnMobileData: true,
      defaultQuality: "SD_360",
      autoplayNext: true,
      notificationsEnabled: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Spoof",
      email: "spoof@gmail.com",
      streamOnMobileData: true,
      defaultQuality: "AUTO",
      autoplayNext: true,
      notificationsEnabled: true,
      createdAt: created,
      updatedAt: created,
    })
  );
});
