// users/{uid}/customWorkouts, workoutLogs and schedules, written the way the services write them.
import { assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc, deleteDoc, writeBatch, arrayUnion, arrayRemove, deleteField }
  from 'firebase/firestore';
import { env, check, finish, NOW } from './harness.mjs';

const uid = 'alice';
const db = env.authenticatedContext(uid).firestore();
const bob = env.authenticatedContext('bob').firestore();
const at = (...path) => doc(db, 'users', uid, ...path);

await env.clearFirestore();

// ---------- customWorkouts (CustomWorkoutService) ----------
const entry = (n) => ({ exerciseName: 'Push-up', position: n, sets: 3, reps: null, seconds: null, restSeconds: 60,
  restAfterSeconds: null });
const entries = (count) => Array.from({ length: count }, (_, i) => entry(i + 1));
const workout = (extra = {}) => ({ name: 'Morning', updatedAt: NOW, exercises: entries(2), ...extra });

await check('custom workout create', assertSucceeds(setDoc(at('customWorkouts', 'w1'), workout())));
await check('custom workout rename', assertSucceeds(updateDoc(at('customWorkouts', 'w1'), { name: 'Evening', updatedAt: NOW + 1 })));
await check('custom workout replace exercises', assertSucceeds(updateDoc(at('customWorkouts', 'w1'), { exercises: entries(1), updatedAt: NOW + 2 })));
await check('custom workout with a rest before the next exercise', assertSucceeds(setDoc(at('customWorkouts', 'w6'),
  workout({ name: 'Rests', exercises: [{ ...entry(1), restAfterSeconds: 90 }, entry(2)] }))));
await check('custom workout with a description', assertSucceeds(setDoc(at('customWorkouts', 'w7'),
  workout({ name: 'Described', description: 'Push day, about 20 minutes.' }))));
await check('custom workout description edit', assertSucceeds(updateDoc(at('customWorkouts', 'w7'),
  { description: 'x'.repeat(200), updatedAt: NOW + 1 })));
await check('custom workout description removed', assertSucceeds(updateDoc(at('customWorkouts', 'w7'),
  { description: deleteField(), updatedAt: NOW + 2 })));
await check('custom workout 201-character description rejected', assertFails(setDoc(at('customWorkouts', 'w2'),
  workout({ description: 'x'.repeat(201) }))));
await check('custom workout description as a number rejected', assertFails(setDoc(at('customWorkouts', 'w2'),
  workout({ description: 5 }))));
await check('custom workout with 50 exercises', assertSucceeds(setDoc(at('customWorkouts', 'w3'), workout({ name: 'Big', exercises: entries(50) }))));
await check('custom workout empty name rejected', assertFails(setDoc(at('customWorkouts', 'w2'), workout({ name: '' }))));
await check('custom workout 41-character name rejected', assertFails(setDoc(at('customWorkouts', 'w2'), workout({ name: 'x'.repeat(41) }))));
await check('custom workout without exercises rejected', assertFails(setDoc(at('customWorkouts', 'w2'), workout({ exercises: [] }))));
await check('custom workout with 51 exercises rejected', assertFails(setDoc(at('customWorkouts', 'w2'), workout({ exercises: entries(51) }))));
await check('custom workout extra field rejected', assertFails(setDoc(at('customWorkouts', 'w2'), workout({ public: true }))));
await check("custom workout of someone else rejected", assertFails(setDoc(doc(bob, 'users', uid, 'customWorkouts', 'w9'), workout())));

// ---------- workoutLogs (WorkoutLogService.saveSession) ----------
const log = (extra = {}) => ({ workoutName: 'Morning', startedAt: NOW, durationSeconds: 600, kcal: 42.5,
  status: 'Completed', exercises: [{ exerciseName: 'Push-up', met: 3.8, timed: false, sets: 3,
    completedSets: 3, reps: 10, seconds: null, perSide: false, activeSeconds: 90, kcal: 42.5 }], ...extra });
const logWithout = (field) => { const l = log(); delete l[field]; return l; };

await check('log create', assertSucceeds(setDoc(at('workoutLogs', 'l1'), log())));
await check('Partial log with zero kcal', assertSucceeds(setDoc(at('workoutLogs', 'l2'), log({ kcal: 0, status: 'Partial' }))));
await check('log update rejected (logs never change)', assertFails(updateDoc(at('workoutLogs', 'l1'), { kcal: 1000 })));
await check('log with unknown status rejected', assertFails(setDoc(at('workoutLogs', 'l3'), log({ status: 'Done' }))));
await check('log with negative duration rejected', assertFails(setDoc(at('workoutLogs', 'l3'), log({ durationSeconds: -1 }))));
await check('log with negative kcal rejected', assertFails(setDoc(at('workoutLogs', 'l3'), log({ kcal: -5 }))));
await check('log without exercises rejected', assertFails(setDoc(at('workoutLogs', 'l3'), logWithout('exercises'))));
await check('log delete', assertSucceeds(deleteDoc(at('workoutLogs', 'l2'))));
await check("reading someone else's log rejected", assertFails(getDoc(doc(bob, 'users', uid, 'workoutLogs', 'l1'))));

// ---------- schedules (ScheduleService, WorkoutLogService) ----------
await check('premade schedule create', assertSucceeds(setDoc(at('schedules', 's1'), { workoutName: 'Beginner Full Body', scheduledAt: NOW })));
await check('repeating schedule create', assertSucceeds(setDoc(at('schedules', 's2'), { workoutName: 'Legs', scheduledAt: NOW, recurrenceRule: 'FREQ=DAILY;INTERVAL=2' })));
await check('custom schedule create', assertSucceeds(setDoc(at('schedules', 's3'), { customWorkoutId: 'w1', scheduledAt: NOW })));
await check('schedule for a missing custom workout rejected', assertFails(setDoc(at('schedules', 's4'), { customWorkoutId: 'nope', scheduledAt: NOW })));
{
  const batch = writeBatch(db);
  batch.set(at('customWorkouts', 'w5'), workout({ name: 'Fresh' }));
  batch.set(at('schedules', 's5'), { customWorkoutId: 'w5', scheduledAt: NOW });
  await check('custom workout and its schedule in one batch', assertSucceeds(batch.commit()));
}
await check('schedule with both workouts rejected', assertFails(setDoc(at('schedules', 's6'), { workoutName: 'Legs', customWorkoutId: 'w1', scheduledAt: NOW })));
await check('schedule without a workout rejected', assertFails(setDoc(at('schedules', 's6'), { scheduledAt: NOW })));
await check('schedule at time 0 rejected', assertFails(setDoc(at('schedules', 's6'), { workoutName: 'Legs', scheduledAt: 0 })));
for (const rule of ['FREQ=WEEKLY', 'FREQ=MONTHLY;INTERVAL=3', 'FREQ=DAILY;INTERVAL=99']) {
  await check(`setRecurrence ${rule}`, assertSucceeds(updateDoc(at('schedules', 's1'), { recurrenceRule: rule })));
}
for (const rule of ['FREQ=YEARLY', 'FREQ=WEEKLY;BYDAY=MO', 'FREQ=DAILY;INTERVAL=0', 'freq=weekly', 7]) {
  await check(`repeat rule ${rule} rejected`, assertFails(updateDoc(at('schedules', 's1'), { recurrenceRule: rule })));
}
await check('setRecurrence(null) removes the rule', assertSucceeds(updateDoc(at('schedules', 's1'), { recurrenceRule: deleteField() })));
await check('reschedule', assertSucceeds(updateDoc(at('schedules', 's1'), { scheduledAt: NOW + 3600 })));
await check('markCompleted', assertSucceeds(updateDoc(at('schedules', 's2'), { completed: arrayUnion(NOW) })));
await check('markNotCompleted', assertSucceeds(updateDoc(at('schedules', 's2'), { completed: arrayRemove(NOW) })));
await check('changing the workout rejected', assertFails(updateDoc(at('schedules', 's1'), { workoutName: 'Arms' })));
await check('switching to a custom workout rejected', assertFails(updateDoc(at('schedules', 's1'), { workoutName: deleteField(), customWorkoutId: 'w1' })));
await check('extra schedule field rejected', assertFails(updateDoc(at('schedules', 's1'), { note: 'hi' })));
{
  // CustomWorkoutService.delete: the workout and its schedules in one batch.
  const batch = writeBatch(db);
  batch.delete(at('customWorkouts', 'w1'));
  batch.delete(at('schedules', 's3'));
  await check('custom workout delete with its schedules', assertSucceeds(batch.commit()));
}

// ---------- anything else under users/{uid} ----------
await check('unknown subcollection rejected', assertFails(setDoc(at('notes', 'n1'), { text: 'hi' })));
await check('nested document rejected', assertFails(setDoc(at('schedules', 's1', 'extra', 'x'), { a: 1 })));

await finish();
