// users/{uid} and usernames/{name}: the profile and the username claim.
import { assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, writeBatch, updateDoc, setDoc } from 'firebase/firestore';
import { env, check, finish, NOW } from './harness.mjs';

const uid = 'alice';
const db = env.authenticatedContext(uid).firestore();

function profile(extra = {}) {
  return { username: 'Alice1', email: 'alice@example.com', height: null, weight: null,
           gender: null, createdAt: NOW, updatedAt: NOW, ...extra };
}

/** Sign-up: the username claim and the profile in one write, like AuthService. */
function signUp(data, name = data.username) {
  const batch = writeBatch(db);
  batch.set(doc(db, 'usernames', String(name).toLowerCase()), { uid });
  batch.set(doc(db, 'users', uid), data);
  return batch.commit();
}

function without(field) {
  const p = profile();
  delete p[field];
  return p;
}

const signUps = [
  ['sign-up with only the required data', true, profile()],
  ['sign-up with height, weight and gender', true, profile({ height: 180.5, weight: 75, gender: 'Female' })],
  ['sign-up without the optional fields', true,
    { username: 'Alice1', email: 'alice@example.com', createdAt: NOW, updatedAt: NOW }],
  ['extra field rejected', false, profile({ admin: true })],
  ['missing createdAt rejected', false, without('createdAt')],
  ['height 10 rejected', false, profile({ height: 10 })],
  ['height as text rejected', false, profile({ height: '180' })],
  ['weight 600 rejected', false, profile({ weight: 600 })],
  ['31-character gender rejected', false, profile({ gender: 'x'.repeat(31) })],
  ['sign-up with a birth year', true, profile({ birthYear: 1995 })],
  ['birth year 1899 rejected', false, profile({ birthYear: 1899 })],
  ['birth year in the future rejected', false, profile({ birthYear: new Date().getUTCFullYear() + 1 })],
  ['fractional birth year rejected', false, profile({ birthYear: 1995.5 })],
  ['birth year as text rejected', false, profile({ birthYear: '1995' })],
  ['fractional time rejected', false, profile({ updatedAt: NOW + 0.5 })],
  ['email over 254 characters rejected', false, profile({ email: 'a'.repeat(250) + '@x.io' })],
];
for (const bad of ['ab', 'a'.repeat(21), 'alice_1', 'al.ice', 'al-ice', 'al ice', 'álice']) {
  signUps.push([`username "${bad}" rejected`, false, profile({ username: bad })]);
}
for (const [name, allowed, data] of signUps) {
  await env.clearFirestore();
  await check(name, (allowed ? assertSucceeds : assertFails)(signUp(data)));
}

// Updates, starting from a valid profile.
await env.clearFirestore();
await assertSucceeds(signUp(profile()));
const me = doc(db, 'users', uid);
await check('profile edit, same username (ProfileService)', assertSucceeds(updateDoc(me,
  { username: 'alice1', height: 170, weight: 60.5, gender: 'Female', birthYear: 1995,
    updatedAt: NOW + 10 })));
await check('email copy at sign-in (AuthService)', assertSucceeds(updateDoc(me,
  { email: 'new@example.com', updatedAt: NOW + 20 })));
await check('changing createdAt rejected', assertFails(updateDoc(me, { createdAt: 1 })));
await check('invalid weight on update rejected', assertFails(updateDoc(me, { weight: 5 })));
await check('removing the username rejected', assertFails(setDoc(me,
  { email: 'alice@example.com', createdAt: NOW, updatedAt: NOW })));
await check("writing someone else's profile rejected", assertFails(setDoc(
  doc(env.authenticatedContext('bob').firestore(), 'users', uid), profile())));

await finish();
