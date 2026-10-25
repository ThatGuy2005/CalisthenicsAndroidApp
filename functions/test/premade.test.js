import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { readInsert, readPremade } from '../seed.js';

const seed = readFileSync(new URL('../../app/src/main/assets/seed.sql', import.meta.url), 'utf8');
const premade = JSON.parse(readFileSync(new URL('../premade.json', import.meta.url), 'utf8'));

test('premade.json matches seed.sql (run `npm run premade` after changing seed.sql)', () => {
  assert.deepEqual(premade, readPremade(seed));
});

test('every premade workout is there, with its exercises in order', () => {
  assert.deepEqual(Object.keys(premade.workouts).sort(), [
    'Advanced Calisthenics', 'Beginner Full Body', 'Cardio Blast', 'Core Basics',
    'Lower Body Burn', 'Upper Body Strength']);
  assert.deepEqual(premade.workouts['Core Basics'].map((e) => e.exerciseName),
    ['Crunches', 'Plank', 'Superman Hold', 'Glute Bridge']);
  for (const list of Object.values(premade.workouts)) {
    for (const e of list) assert.ok(premade.exercises[e.exerciseName], e.exerciseName);
  }
});

test('exercise flags and defaults are read', () => {
  assert.deepEqual(premade.exercises['Jumping Jacks'],
    { timed: true, defaultSeconds: 30, defaultReps: null, perSide: false });
  assert.deepEqual(premade.exercises['Archer Push-Up'],
    { timed: false, defaultSeconds: null, defaultReps: 6, perSide: true });
});

test('SQL text with quotes, NULLs, numbers and comments is read', () => {
  const sql = "-- intro\nINSERT INTO T (A, B, C) VALUES\n-- note\n('it''s, (fine)', NULL, 3.5),\n('x', 2, -1);";
  assert.deepEqual(readInsert(sql, 'T'), [
    { A: "it's, (fine)", B: null, C: 3.5 },
    { A: 'x', B: 2, C: -1 },
  ]);
});
