// End-to-end check of emailNewSchedule and deleteEmailRecords in the Auth, Firestore and
// Functions emulators: a new schedule must be emailed once, and an account's records go with it. In the emulator nothing is really sent (see index.js), so
// this checks the mail record. Run with `npm run test:emulator`.
import assert from 'node:assert/strict';
import { initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { getFirestore } from 'firebase-admin/firestore';

const app = initializeApp({ projectId: 'demo-calisthenics' });
const db = getFirestore(app);
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function recordFor(uid, scheduleId, state = 'SENT') {
  const ref = db.doc(`mail/schedule-${uid}-${scheduleId}`);
  for (let i = 0; i < 60; i++) {                 // the trigger runs asynchronously
    const doc = await ref.get();
    if (doc.exists && doc.get('state') === state) return doc.data();
    await sleep(500);
  }
  throw new Error(`No ${state} record for ${scheduleId}`);
}

const { uid } = await getAuth(app).createUser({ email: 'alice@example.com', password: 'secret123' });
// The profile's copy of the address is ignored: the email goes to the sign-in address.
await db.doc(`users/${uid}`).set({ username: 'alice', email: 'a@x.com, b@y.com', createdAt: 1, updatedAt: 1 });
await db.doc(`users/${uid}/customWorkouts/w1`).set({
  name: 'Morning', updatedAt: 1,
  exercises: [{ exerciseName: 'Plank', position: 1, sets: 2, reps: null, seconds: null, restSeconds: 30, restAfterSeconds: null }],
});

await db.doc(`users/${uid}/schedules/s1`).set({ workoutName: 'Core Basics', scheduledAt: 1792080000, timeZone: 'Europe/Bucharest' });
const premade = await recordFor(uid, 's1');
assert.deepEqual(Object.keys(premade).sort(), ['createdAt', 'sentAt', 'state']);   // no address or text kept
console.log('PASS premade workout emailed, only the outcome kept');

await db.doc(`users/${uid}/schedules/s2`).set({ customWorkoutId: 'w1', scheduledAt: 1792080000, recurrenceRule: 'FREQ=WEEKLY' });
await recordFor(uid, 's2');
console.log('PASS custom workout emailed');

await db.doc(`users/${uid}/schedules/s2`).update({ scheduledAt: 1792083600 });
await db.doc(`users/${uid}/schedules/s3`).set({ workoutName: 'No Such Workout', scheduledAt: 1792080000 });
await sleep(3000);
assert.deepEqual((await db.collection('mail').get()).docs.map((d) => d.id).sort(),
  [`schedule-${uid}-s1`, `schedule-${uid}-s2`]);
console.log('PASS changing a schedule or an unknown workout sends nothing');

// Another user's record must survive alice's account deletion.
await db.doc('mail/schedule-bob123-s9').set({ state: 'SENT' });
await db.doc(`users/${uid}`).delete();
for (let i = 0; i < 60 && (await db.collection('mail').get()).size > 1; i++) await sleep(500);
assert.deepEqual((await db.collection('mail').get()).docs.map((d) => d.id), ['schedule-bob123-s9']);
console.log('PASS deleting the account deletes its mail records only');
process.exit(0);
