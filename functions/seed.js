// Reads the premade exercises and workouts out of the app's seed.sql, so the server lists the
// same exercises as the app. Only the INSERT statements this needs are read.

/** The rows of `INSERT INTO <table> (columns) VALUES (...), (...);` as objects by column. */
export function readInsert(sql, table) {
  const start = sql.search(new RegExp(`INSERT INTO ${table}\\s*\\(`));
  if (start < 0) throw new Error(`No INSERT INTO ${table} in seed.sql`);
  const open = sql.indexOf('(', start);
  const columns = sql.slice(open + 1, sql.indexOf(')', open)).split(',').map((c) => c.trim());
  const values = sql.indexOf('VALUES', open);
  const rows = [];
  let row = null;
  let i = values + 'VALUES'.length;
  while (i < sql.length) {
    const ch = sql[i];
    if (ch === '-' && sql[i + 1] === '-') {            // comment to the end of the line
      i = sql.indexOf('\n', i);
      if (i < 0) break;
    } else if (ch === '(') {
      row = [];
      i++;
    } else if (ch === ')') {
      rows.push(Object.fromEntries(columns.map((name, n) => [name, row[n]])));
      row = null;
      i++;
    } else if (ch === ';' && row === null) {
      break;
    } else if (ch === "'") {                           // text; '' is a quote
      let text = '';
      i++;
      for (;;) {
        if (sql[i] === "'" && sql[i + 1] === "'") { text += "'"; i += 2; }
        else if (sql[i] === "'") { i++; break; }
        else text += sql[i++];
      }
      row.push(text);
    } else if (/[0-9.-]/.test(ch) && row !== null) {
      const match = /^-?[0-9]+(\.[0-9]+)?/.exec(sql.slice(i));
      row.push(Number(match[0]));
      i += match[0].length;
    } else if (sql.startsWith('NULL', i) && row !== null) {
      row.push(null);
      i += 4;
    } else {
      i++;                                             // commas and white space
    }
  }
  return rows;
}

/** What the email needs: exercises by name and each premade workout's exercises in order. */
export function readPremade(sql) {
  const exercises = {};
  for (const e of readInsert(sql, 'Exercise')) {
    exercises[e.Name] = {
      timed: e.IsTimed === 1,
      defaultSeconds: e.DefaultSeconds,
      defaultReps: e.DefaultReps,
      perSide: e.IsPerSide === 1,
    };
  }
  const workouts = {};
  for (const w of readInsert(sql, 'Workout')) workouts[w.Name] = [];
  for (const o of readInsert(sql, 'ExerciseOrder')) {
    workouts[o.WorkoutName].push({
      exerciseName: o.ExerciseName,
      position: o.Position,
      sets: o.Sets,
      reps: o.Reps,
      seconds: o.Seconds,
      restSeconds: o.RestSeconds,
      restAfterSeconds: o.RestAfterSeconds,
    });
  }
  for (const list of Object.values(workouts)) list.sort((a, b) => a.position - b.position);
  return { exercises, workouts };
}
