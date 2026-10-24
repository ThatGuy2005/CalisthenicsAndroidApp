// End-to-end check of emailNewSchedule in the Firestore and Functions emulators: a new schedule
// must produce one `mail` document. Run with `npm run test:emulator`.
import assert from 'node:assert/strict';
import { initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';

const db = getFirestore(initializeApp({ projectId: 'demo-calisthenics' }));
const uid = 'alice';

async function mailFor(scheduleId) {
  const ref = db.doc(`mail/schedule-${uid}-${scheduleId}`);
  for (let i = 0; i < 60; i++) {                 // the trigger runs asynchronously
    const doc = await ref.get();
    if (doc.exists) return doc.data();
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error(`No mail for ${scheduleId}`);
}

await db.doc(`users/${uid}`).set({ username: 'alice', email: 'alice@example.com', createdAt: 1, updatedAt: 1 });
await db.doc(`users/${uid}/customWorkouts/w1`).set({
  name: 'Morning', updatedAt: 1,
  exercises: [{ exerciseName: 'Plank', position: 1, sets: 2, reps: null, seconds: null, restSeconds: 30, restAfterSeconds: null }],
});

await db.doc(`users/${uid}/schedules/s1`).set({ workoutName: 'Core Basics', scheduledAt: 1792080000, timeZone: 'Europe/Bucharest' });
const premade = await mailFor('s1');
assert.equal(premade.to, 'alice@example.com');
assert.equal(premade.message.subject, 'Scheduled workout: Core Basics, Thursday, October 15, 2026 at 7:00 PM');
assert.ok(premade.message.text.includes('1. Crunches: 3 x 15 reps, 30 s rest'));
assert.ok(premade.message.attachments[0].content.includes('UID:s1@calisthenicsapp'));
console.log('PASS premade workout email');

await db.doc(`users/${uid}/schedules/s2`).set({ customWorkoutId: 'w1', scheduledAt: 1792080000, recurrenceRule: 'FREQ=WEEKLY' });
const custom = await mailFor('s2');
assert.ok(custom.message.subject.startsWith('Scheduled workout: Morning, Thursday, October 15, 2026 at 4:00 PM'));
assert.ok(custom.message.text.includes('1. Plank: 2 x 30 s, 30 s rest'));
assert.ok(custom.message.text.includes('Repeats: every week'));
console.log('PASS custom workout email (no time zone: UTC)');

await db.doc(`users/${uid}/schedules/s2`).update({ scheduledAt: 1792083600 });
await new Promise((resolve) => setTimeout(resolve, 3000));
assert.equal((await db.collection('mail').get()).size, 2);
console.log('PASS changing a schedule sends nothing');
process.exit(0);
