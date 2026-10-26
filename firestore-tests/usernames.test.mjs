// usernames/{name} together with users/{uid}: a user holds exactly the claim of the username
// their profile has, through username changes and account deletion.
import { assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, writeBatch, updateDoc, setDoc, deleteDoc } from 'firebase/firestore';
import { env, check, finish, NOW } from './harness.mjs';

const uid = 'alice';
const db = env.authenticatedContext(uid).firestore();
const me = doc(db, 'users', uid);
const claim = (name) => doc(db, 'usernames', name);

function profile(username) {
  return { username, email: 'alice@example.com', createdAt: NOW, updatedAt: NOW };
}

/** A fresh database with alice signed up as Alice1, like AuthService. */
async function signedUp() {
  await env.clearFirestore();
  const batch = writeBatch(db);
  batch.set(claim('alice1'), { uid });
  batch.set(me, profile('Alice1'));
  await assertSucceeds(batch.commit());
}

/** A fresh database with alice's profile but no claim: an account from before usernames/. */
async function signedUpWithoutClaim() {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled((context) =>
    setDoc(doc(context.firestore(), 'users', uid), profile('Alice1')));
}

/** ProfileService.changeUsername: optionally release the old claim, claim the new one, update. */
function changeUsername(newName, { releaseOld = 'alice1' } = {}) {
  const batch = writeBatch(db);
  if (releaseOld) batch.delete(claim(releaseOld));
  batch.set(claim(newName.toLowerCase()), { uid });
  batch.update(me, { username: newName, updatedAt: NOW + 10 });
  return batch.commit();
}

await signedUp();
await check('username change releasing the old name (ProfileService)',
  assertSucceeds(changeUsername('Alice2')));

await signedUp();
await check('username change keeping the old name rejected',
  assertFails(changeUsername('Alice2', { releaseOld: null })));

await signedUp();
await check('letter case change keeps the same claim',
  assertSucceeds(updateDoc(me, { username: 'ALICE1', updatedAt: NOW + 10 })));

await signedUpWithoutClaim();
await check('username change of an account without a claim',
  assertSucceeds(changeUsername('Alice2', { releaseOld: null })));

await signedUp();
await check('claiming a second name without using it rejected',
  assertFails(setDoc(claim('spare'), { uid })));

await signedUp();
await check('releasing the name the profile still has rejected',
  assertFails(deleteDoc(claim('alice1'))));

await signedUp();
{
  // AccountService.deleteProfileAndAccount: the claim and the profile in one batch.
  const batch = writeBatch(db);
  batch.delete(claim('alice1'));
  batch.delete(me);
  await check('account delete releasing the name (AccountService)', assertSucceeds(batch.commit()));
}

await signedUp();
await check('profile delete keeping the name rejected', assertFails(deleteDoc(me)));

await signedUpWithoutClaim();
await check('profile delete of an account without a claim', assertSucceeds(deleteDoc(me)));

// Someone else's name.
await signedUp();
{
  const bobDb = env.authenticatedContext('bob').firestore();
  const batch = writeBatch(bobDb);
  batch.set(doc(bobDb, 'usernames', 'bob1'), { uid: 'bob' });
  batch.set(doc(bobDb, 'users', 'bob'), { ...profile('Bob1'), email: 'bob@example.com' });
  await assertSucceeds(batch.commit());
}
await check("taking a name someone else holds rejected", assertFails(changeUsername('Bob1')));
await check("releasing someone else's name rejected", assertFails(deleteDoc(claim('bob1'))));

await finish();
