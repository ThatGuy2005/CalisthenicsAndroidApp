// The email of a newly scheduled workout (FR-6.3): the server's copy of the app's
// Scheduling/WorkoutSummary, Session/WorkoutExercises and Scheduling/IcsWriter, so the email
// says the same as the one the app builds. Keep them in step.

/** Reminder before the workout in the .ics file, as WorkoutSummary.REMINDER_MINUTES. */
export const REMINDER_MINUTES = 30;
const MIN_EVENT_SECONDS = 15 * 60;
const ROUND_TO_SECONDS = 5 * 60;
const SECONDS_PER_REP = 3;
const FALLBACK_REPS = 10;
const FALLBACK_SECONDS = 30;

/**
 * A workout's exercises with every target filled in, like WorkoutExercises.toSessionExercise:
 * the exercise's own default when the workout leaves reps or seconds open, and a missing rest
 * before the next exercise is the rest between sets. entries are premade.json's or a custom
 * workout document's exercises; library is premade.json's exercises.
 */
export function sessionExercises(entries, library) {
  return [...entries]
    .sort((a, b) => (a.position ?? 0) - (b.position ?? 0))
    .map((e) => {
      const exercise = library[e.exerciseName] ?? { timed: false, defaultReps: null, perSide: false };
      const rest = e.restSeconds ?? 0;
      return {
        name: e.exerciseName,
        timed: exercise.timed,
        targetReps: exercise.timed ? null : e.reps ?? exercise.defaultReps ?? FALLBACK_REPS,
        targetSeconds: exercise.timed ? e.seconds ?? exercise.defaultSeconds ?? FALLBACK_SECONDS : null,
        sets: e.sets ?? 1,
        restSeconds: rest,
        restAfterSeconds: e.restAfterSeconds ?? rest,
        perSide: exercise.perSide,
      };
    });
}

/** WorkoutExercises.estimateSeconds: every set, the rests between them and between exercises. */
export function estimateSeconds(exercises) {
  let total = 0;
  for (const e of exercises) {
    const sides = e.perSide ? 2 : 1;
    const work = e.timed ? e.targetSeconds * sides : e.targetReps * SECONDS_PER_REP * sides;
    total += e.sets * work + (e.sets - 1) * e.restSeconds + e.restAfterSeconds;
  }
  if (exercises.length > 0) total -= exercises[exercises.length - 1].restAfterSeconds;
  return Math.max(total, 0);
}

/** Recurrence.describe for a rule like FREQ=WEEKLY or FREQ=DAILY;INTERVAL=2; null if none. */
export function describeRule(rule) {
  if (!rule) return null;
  const parts = Object.fromEntries(rule.split(';').map((p) => p.split('=')));
  const unit = { DAILY: 'day', WEEKLY: 'week', MONTHLY: 'month' }[parts.FREQ];
  const interval = Number(parts.INTERVAL ?? 1);
  return interval === 1 ? `Every ${unit}` : `Every ${interval} ${unit}s`;
}

/** WorkoutSummary.getExerciseList: one numbered line per exercise. */
export function exerciseList(exercises) {
  return exercises.map((e, i) => {
    let line = `${i + 1}. ${e.name}: ${e.sets} x `
      + (e.timed ? `${e.targetSeconds} s` : `${e.targetReps} reps`);
    if (e.perSide) line += ' per side';
    if (e.restSeconds > 0) line += `, ${e.restSeconds} s rest`;
    const last = i === exercises.length - 1;
    if (!last && e.restAfterSeconds !== e.restSeconds) {
      line += `, ${e.restAfterSeconds} s before the next exercise`;
    }
    return line;
  }).join('\n');
}

/** Like the app's DateFormat FULL date + SHORT time, in the user's time zone. */
export function formatStart(startSeconds, timeZone) {
  return new Intl.DateTimeFormat('en-US', { dateStyle: 'full', timeStyle: 'short', timeZone })
    .format(new Date(startSeconds * 1000));
}

/** A time zone id Intl knows, else UTC (older schedules have none). */
export function safeTimeZone(timeZone) {
  try {
    if (timeZone) {
      new Intl.DateTimeFormat('en-US', { timeZone });
      return timeZone;
    }
  } catch {
    // unknown id
  }
  return 'UTC';
}

/**
 * The email for one schedule: { subject, text, ics }. workout is { name, exercises } with
 * exercises from sessionExercises; schedule is the Firestore document's data.
 */
export function buildEmail(scheduleId, schedule, workout, nowSeconds) {
  const timeZone = safeTimeZone(schedule.timeZone);
  const start = formatStart(schedule.scheduledAt, timeZone);
  const estimated = estimateSeconds(workout.exercises);
  const minutes = Math.max(1, Math.ceil(estimated / 60));
  const repeats = describeRule(schedule.recurrenceRule);
  const repeatsLine = repeats ? `\nRepeats: ${repeats.toLowerCase()}` : '';
  const list = exerciseList(workout.exercises);

  const subject = `Scheduled workout: ${workout.name}, ${start}`;
  const text = `Workout: ${workout.name}\n`
    + `When: ${start} (about ${minutes} min)${repeatsLine}\n\n`
    + `Exercises:\n${list}\n\n`
    + 'Sent from CalisthenicsApp.\n';

  const eventSeconds = Math.max(
    Math.ceil(estimated / ROUND_TO_SECONDS) * ROUND_TO_SECONDS, MIN_EVENT_SECONDS);
  const description = `About ${minutes} min.${repeatsLine}\n\nExercises:\n${list}`;
  const ics = icsEvent({
    uid: `${scheduleId}@calisthenicsapp`,
    startSeconds: schedule.scheduledAt,
    endSeconds: schedule.scheduledAt + eventSeconds,
    rrule: schedule.recurrenceRule ?? null,
    summary: `Workout: ${workout.name}`,
    description,
    nowSeconds,
  });
  return { subject, text, ics };
}

// ---------- .ics, as Scheduling/IcsWriter (RFC 5545) ----------

const CRLF = '\r\n';
const MAX_LINE_OCTETS = 75;

/** Text values: backslash, semicolon and comma are escaped, line breaks become \n. */
export function escapeText(text) {
  return (text ?? '')
    .replaceAll('\\', '\\\\')
    .replaceAll(';', '\\;')
    .replaceAll(',', '\\,')
    .replaceAll('\r\n', '\\n')
    .replaceAll('\n', '\\n')
    .replaceAll('\r', '\\n');
}

/** Lines of at most 75 UTF-8 octets, continuation lines start with a space. */
export function fold(line) {
  let out = '';
  let octets = 0;
  for (const ch of line) {
    const size = Buffer.byteLength(ch, 'utf8');
    if (octets + size > MAX_LINE_OCTETS) {
      out += CRLF + ' ';
      octets = 1;
    }
    out += ch;
    octets += size;
  }
  return out;
}

export function utc(epochSeconds) {
  return new Date(epochSeconds * 1000).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '');
}

function icsEvent({ uid, startSeconds, endSeconds, rrule, summary, description, nowSeconds }) {
  const lines = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//CalisthenicsApp//Scheduled workout//EN',
    'CALSCALE:GREGORIAN',
    'METHOD:PUBLISH',
    'BEGIN:VEVENT',
    `UID:${escapeText(uid)}`,
    `DTSTAMP:${utc(nowSeconds)}`,
    `DTSTART:${utc(startSeconds)}`,
    `DTEND:${utc(endSeconds)}`,
    ...(rrule ? [`RRULE:${rrule}`] : []),
    `SUMMARY:${escapeText(summary)}`,
    `DESCRIPTION:${escapeText(description)}`,
    'BEGIN:VALARM',
    'ACTION:DISPLAY',
    `DESCRIPTION:${escapeText(summary)}`,
    `TRIGGER:-PT${REMINDER_MINUTES}M`,
    'END:VALARM',
    'END:VEVENT',
    'END:VCALENDAR',
  ];
  return lines.map((line) => fold(line) + CRLF).join('');
}
