// Shared setup for the rules tests: loads ../firestore.rules into the running emulator and
// counts the results. Each test file ends with finish(), which sets the exit code.
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';

const rulesPath = fileURLToPath(new URL('../firestore.rules', import.meta.url));

export const env = await initializeTestEnvironment({
  projectId: 'demo-calisthenics',
  firestore: { rules: readFileSync(rulesPath, 'utf8'), host: '127.0.0.1', port: 8080 },
});

/** Epoch seconds, like the app writes. */
export const NOW = 1792080000;

let failures = 0;

/** Awaits one assertSucceeds/assertFails and prints PASS or FAIL. */
export async function check(name, promise) {
  try {
    await promise;
    console.log('PASS', name);
  } catch (e) {
    failures++;
    console.log('FAIL', name, '-', e.message);
  }
}

export async function finish() {
  await env.cleanup();
  console.log(failures === 0 ? 'ALL PASSED' : `${failures} FAILED`);
  process.exit(failures === 0 ? 0 : 1);
}
