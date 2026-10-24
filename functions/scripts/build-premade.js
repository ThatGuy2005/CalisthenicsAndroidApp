// Writes premade.json from the app's seed.sql. Run `npm run premade` after changing seed.sql;
// a test fails while the two differ.
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { readPremade } from '../seed.js';

const seed = fileURLToPath(new URL('../../app/src/main/assets/seed.sql', import.meta.url));
const out = fileURLToPath(new URL('../premade.json', import.meta.url));
writeFileSync(out, JSON.stringify(readPremade(readFileSync(seed, 'utf8')), null, 2) + '\n');
console.log(`Wrote ${out}`);
