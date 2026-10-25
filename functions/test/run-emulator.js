// Runs test/emulator.check.js against the Auth, Firestore and Functions emulators (`npm run test:emulator`).
import { spawnSync } from 'node:child_process';

const PORT = 8080;   // must match ../firebase.json

// As in ../firestore-tests/run.mjs: on Windows the Firestore emulator's Java process outlives
// firebase-tools and keeps the port busy, so stop exactly that process before and after.
function stopLeftoverEmulator() {
  if (process.platform !== 'win32') return;
  const script = "Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\""
    + ` | Where-Object { $_.CommandLine -like '*cloud-firestore-emulator*--port ${PORT}*' }`
    + ' | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }';
  spawnSync('powershell', ['-NoProfile', '-Command', script], { stdio: 'inherit' });
}

stopLeftoverEmulator();
const result = spawnSync(
  'firebase emulators:exec --only auth,functions,firestore --project demo-calisthenics "node test/emulator.check.js"',
  { stdio: 'inherit', shell: true });
stopLeftoverEmulator();
process.exit(result.status ?? 1);
