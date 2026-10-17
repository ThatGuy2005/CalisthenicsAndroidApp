// Runs every rules test in the Firestore emulator. Use it through `npm test`.
import { spawnSync } from 'node:child_process';

const PORT = 8080;   // must match firebase.json
const TESTS = ['profile.test.mjs', 'subcollections.test.mjs'];

// On Windows, firebase-tools stops the emulator's wrapper but leaves its Java process running,
// which keeps the port busy and makes the next run fail. Stop exactly that process: a java.exe
// running the Firestore emulator jar on our port.
function stopLeftoverEmulator() {
  if (process.platform !== 'win32') return;
  const script = "Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\""
    + ` | Where-Object { $_.CommandLine -like '*cloud-firestore-emulator*--port ${PORT}*' }`
    + ' | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }';
  spawnSync('powershell', ['-NoProfile', '-Command', script], { stdio: 'inherit' });
}

stopLeftoverEmulator();
const run = TESTS.map((file) => `node ${file}`).join(' && ');
const result = spawnSync(
  `firebase emulators:exec --only firestore --project demo-calisthenics "${run}"`,
  { stdio: 'inherit', shell: true });
stopLeftoverEmulator();
process.exit(result.status ?? 1);
