// The same expectations as the app's WorkoutSummaryTest and IcsWriterTest, so the server's
// email says what the app's does.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildEmail, describeRule, escapeText, estimateSeconds, exerciseList, fold, formatStart,
  safeTimeZone, sessionExercises, utc,
} from '../email.js';

const START = 1792080000;   // 2026-10-15 16:00:00 UTC

const reps = (name, sets, count, rest, restAfter = rest) => ({
  name, timed: false, targetReps: count, targetSeconds: null, sets, restSeconds: rest,
  restAfterSeconds: restAfter, perSide: false,
});
const timed = (name, sets, seconds, rest, restAfter = rest) => ({
  name, timed: true, targetReps: null, targetSeconds: seconds, sets, restSeconds: rest,
  restAfterSeconds: restAfter, perSide: false,
});

test('estimate counts sets and rests but not the last rest', () => {
  assert.equal(estimateSeconds([reps('Push-up', 3, 10, 60), timed('Plank', 2, 30, 30)]), 360);
});

test('per-side exercises count both sides', () => {
  assert.equal(estimateSeconds([{ ...reps('Lunges', 1, 10, 0), perSide: true }]), 60);
  assert.equal(estimateSeconds([{ ...timed('Side plank', 1, 20, 0), perSide: true }]), 40);
});

test('exercise list has one numbered line per exercise', () => {
  assert.equal(exerciseList([reps('Push-up', 3, 10, 60), timed('Plank', 2, 30, 0)]),
    '1. Push-up: 3 x 10 reps, 60 s rest\n2. Plank: 2 x 30 s');
});

test('a different rest before the next exercise is listed', () => {
  assert.equal(exerciseList([reps('Push-up', 3, 10, 60, 90), timed('Plank', 2, 30, 30, 90)]),
    '1. Push-up: 3 x 10 reps, 60 s rest, 90 s before the next exercise\n'
    + '2. Plank: 2 x 30 s, 30 s rest');
});

test('per-side targets say so', () => {
  assert.equal(exerciseList([{ ...reps('Lunges', 3, 12, 45), perSide: true }]),
    '1. Lunges: 3 x 12 reps per side, 45 s rest');
});

test('targets left open use the exercise defaults', () => {
  const library = {
    Plank: { timed: true, defaultSeconds: 30, defaultReps: null, perSide: false },
    Squat: { timed: false, defaultSeconds: null, defaultReps: 15, perSide: false },
  };
  const [plank, squat, unknown] = sessionExercises([
    { exerciseName: 'Squat', position: 2, sets: 3, reps: null, seconds: null, restSeconds: 45, restAfterSeconds: null },
    { exerciseName: 'Plank', position: 1, sets: 2, reps: 99, seconds: null, restSeconds: 30, restAfterSeconds: 60 },
    { exerciseName: 'Gone', position: 3, sets: null, reps: null, seconds: null, restSeconds: null, restAfterSeconds: null },
  ], library);
  assert.deepEqual(plank, timed('Plank', 2, 30, 30, 60));          // reps ignored for a timed one
  assert.deepEqual(squat, reps('Squat', 3, 15, 45));                // rest after = rest
  assert.deepEqual(unknown, reps('Gone', 1, 10, 0));                // fallbacks
});

test('repeat rules are described like Recurrence.describe', () => {
  assert.equal(describeRule(null), null);
  assert.equal(describeRule('FREQ=WEEKLY'), 'Every week');
  assert.equal(describeRule('FREQ=DAILY;INTERVAL=2'), 'Every 2 days');
  assert.equal(describeRule('FREQ=MONTHLY'), 'Every month');
});

test('the date is shown in the user\'s time zone', () => {
  assert.equal(formatStart(START, 'UTC'), 'Thursday, October 15, 2026 at 4:00 PM');
  assert.equal(formatStart(START, 'Europe/Bucharest'), 'Thursday, October 15, 2026 at 7:00 PM');
});

test('a missing or unknown time zone falls back to UTC', () => {
  assert.equal(safeTimeZone(undefined), 'UTC');
  assert.equal(safeTimeZone('Not/AZone'), 'UTC');
  assert.equal(safeTimeZone('Europe/Bucharest'), 'Europe/Bucharest');
});

test('email has name, time and exercises', () => {
  const email = buildEmail('s1', { scheduledAt: START, timeZone: 'Europe/Bucharest' },
    { name: 'Legs', exercises: [reps('Squat', 3, 15, 45)] }, START);
  assert.equal(email.subject, 'Scheduled workout: Legs, Thursday, October 15, 2026 at 7:00 PM');
  assert.equal(email.text,
    'Workout: Legs\n'
    + 'When: Thursday, October 15, 2026 at 7:00 PM (about 4 min)\n\n'
    + 'Exercises:\n1. Squat: 3 x 15 reps, 45 s rest\n\n'
    + 'Sent from CalisthenicsApp.\n');
});

test('a repeating workout says how it repeats', () => {
  const email = buildEmail('abc123', { scheduledAt: START, recurrenceRule: 'FREQ=WEEKLY' },
    { name: 'Legs', exercises: [reps('Squat', 3, 15, 45)] }, START);
  assert.ok(email.text.includes('Repeats: every week'));
  assert.ok(email.ics.includes('\r\nRRULE:FREQ=WEEKLY\r\n'));
});

test('the .ics has the event, a reminder and no bare line feeds', () => {
  const { ics } = buildEmail('abc123', { scheduledAt: START },
    { name: 'Legs', exercises: [reps('Squat', 3, 15, 45)] }, START - 3600);
  assert.ok(ics.startsWith('BEGIN:VCALENDAR\r\nVERSION:2.0\r\n'));
  assert.ok(ics.endsWith('END:VEVENT\r\nEND:VCALENDAR\r\n'));
  assert.ok(ics.includes('\r\nUID:abc123@calisthenicsapp\r\n'));
  assert.ok(ics.includes('\r\nDTSTAMP:20261015T150000Z\r\n'));
  assert.ok(ics.includes('\r\nDTSTART:20261015T160000Z\r\n'));
  assert.ok(ics.includes('\r\nDTEND:20261015T161500Z\r\n'));          // at least 15 minutes
  assert.ok(ics.includes('\r\nSUMMARY:Workout: Legs\r\n'));
  assert.ok(ics.includes('\r\nTRIGGER:-PT30M\r\n'));
  assert.ok(!ics.includes('RRULE'));
  assert.ok(!ics.replaceAll('\r\n', '').includes('\n'));
});

test('the event is rounded up to whole 5 minutes', () => {
  const { ics } = buildEmail('s1', { scheduledAt: START },
    { name: 'Long', exercises: [timed('Plank', 10, 60, 60), reps('Squat', 2, 20, 10)] }, START);
  assert.ok(ics.includes('\r\nDTEND:20261015T162500Z\r\n'));          // 1330 s -> 25 min
});

test('.ics text is escaped and long lines are folded', () => {
  assert.equal(escapeText('a\\b;c,d\ne\r\nf'), 'a\\\\b\\;c\\,d\\ne\\nf');
  assert.equal(utc(START), '20261015T160000Z');
  const line = 'DESCRIPTION:' + 'Húzódzkodás, '.repeat(20);
  const folded = fold(line);
  for (const part of folded.split('\r\n')) assert.ok(Buffer.byteLength(part, 'utf8') <= 75);
  assert.equal(folded.replaceAll('\r\n ', ''), line);
  assert.equal(fold('SUMMARY:Workout'), 'SUMMARY:Workout');
});
