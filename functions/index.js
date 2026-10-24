// Emails every newly scheduled workout to its user (FR-6.3), through Gmail's SMTP server with
// the sender's app password from Secret Manager (GMAIL_APP_PASSWORD).
// mail/schedule-{uid}-{scheduleId} records that a schedule's email was handled, so a retried
// event sends nothing more. Clients cannot read or write `mail`: firestore.rules denies
// everything it does not list.
import { readFileSync } from 'node:fs';
import { initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { FieldValue, getFirestore } from 'firebase-admin/firestore';
import { logger } from 'firebase-functions';
import { defineSecret, defineString } from 'firebase-functions/params';
import { onDocumentCreated } from 'firebase-functions/v2/firestore';
import nodemailer from 'nodemailer';
import { buildEmail, isPlainAddress, sessionExercises } from './email.js';

initializeApp();

/** The Gmail address the emails come from; set in .env. */
const SENDER_EMAIL = defineString('SENDER_EMAIL');
/** An app password of that Gmail account: `firebase functions:secrets:set GMAIL_APP_PASSWORD`. */
const GMAIL_APP_PASSWORD = defineSecret('GMAIL_APP_PASSWORD');

/** The premade exercises and workouts; see scripts/build-premade.js. */
const premade = JSON.parse(readFileSync(new URL('./premade.json', import.meta.url), 'utf8'));

/** In the emulator nothing is sent: the email is only built (test/emulator.check.js). */
function transport() {
  if (process.env.FUNCTIONS_EMULATOR === 'true') return nodemailer.createTransport({ jsonTransport: true });
  return nodemailer.createTransport({
    host: 'smtp.gmail.com',
    port: 465,
    secure: true,
    auth: { user: SENDER_EMAIL.value(), pass: GMAIL_APP_PASSWORD.value() },
  });
}

export const emailNewSchedule = onDocumentCreated(
  // Firestore is in eur3; its functions run in europe-west1.
  { document: 'users/{uid}/schedules/{scheduleId}', region: 'europe-west1', secrets: [GMAIL_APP_PASSWORD] },
  async (event) => {
    const schedule = event.data?.data();
    if (!schedule) return;
    const { uid, scheduleId } = event.params;
    const db = getFirestore();

    // The sign-in address, not the profile's copy: clients can write any text into the
    // profile, and the email must go to exactly this user.
    const to = (await getAuth().getUser(uid)).email;
    if (!isPlainAddress(to)) {
      logger.warn('No usable email address for the schedule', { uid, scheduleId });
      return;
    }

    let workout;
    if (schedule.customWorkoutId) {
      const custom = await db.doc(`users/${uid}/customWorkouts/${schedule.customWorkoutId}`).get();
      if (!custom.exists) {
        logger.warn('The scheduled custom workout is gone', { uid, scheduleId });
        return;
      }
      workout = {
        name: custom.get('name'),
        exercises: sessionExercises(custom.get('exercises') ?? [], premade.exercises),
      };
    } else {
      const entries = premade.workouts[schedule.workoutName];
      if (!entries) {
        logger.warn('Unknown premade workout', { uid, scheduleId, workoutName: schedule.workoutName });
        return;
      }
      workout = { name: schedule.workoutName, exercises: sessionExercises(entries, premade.exercises) };
    }
    const email = buildEmail(scheduleId, schedule, workout, Math.floor(Date.now() / 1000));

    // Claim the schedule's email first: the id is fixed, so a retried event stops here. An
    // email is rather lost than sent twice. Only the outcome is kept, not the address or text.
    const record = db.collection('mail').doc(`schedule-${uid}-${scheduleId}`);
    try {
      await record.create({ state: 'SENDING', createdAt: FieldValue.serverTimestamp() });
    } catch (e) {
      if (e.code === 6) return;   // ALREADY_EXISTS: handled on an earlier try
      throw e;
    }
    try {
      await transport().sendMail({
        from: { name: 'CalisthenicsApp', address: SENDER_EMAIL.value() },
        to,
        subject: email.subject,
        text: email.text,
        attachments: [{
          filename: 'workout.ics',
          content: email.ics,
          contentType: 'text/calendar; charset=utf-8; method=PUBLISH',
        }],
      });
      await record.update({ state: 'SENT', sentAt: FieldValue.serverTimestamp() });
    } catch (e) {
      logger.error('Sending the schedule email failed', { uid, scheduleId, error: e.message });
      await record.update({ state: 'ERROR', error: String(e.message).slice(0, 500) });
    }
  },
);
