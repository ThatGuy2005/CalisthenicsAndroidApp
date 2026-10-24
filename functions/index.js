// Emails every newly scheduled workout to its user (FR-6.3). The email goes into the `mail`
// collection, which the Trigger Email extension (firebase/firestore-send-email) sends.
// Clients cannot read or write `mail`: firestore.rules denies everything it does not list.
import { readFileSync } from 'node:fs';
import { initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { logger } from 'firebase-functions';
import { onDocumentCreated } from 'firebase-functions/v2/firestore';
import { buildEmail, sessionExercises } from './email.js';

initializeApp();

/** The premade exercises and workouts; see scripts/build-premade.js. */
const premade = JSON.parse(readFileSync(new URL('./premade.json', import.meta.url), 'utf8'));

export const emailNewSchedule = onDocumentCreated(
  // Firestore is in eur3; its functions run in europe-west1.
  { document: 'users/{uid}/schedules/{scheduleId}', region: 'europe-west1' },
  async (event) => {
    const schedule = event.data?.data();
    if (!schedule) return;
    const { uid, scheduleId } = event.params;
    const db = getFirestore();

    const profile = await db.doc(`users/${uid}`).get();
    const to = profile.get('email');
    if (!to) {
      logger.warn('No email address for the schedule', { uid, scheduleId });
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
    try {
      // One email per schedule: the id is fixed, so a retried event cannot send it twice.
      await db.collection('mail').doc(`schedule-${uid}-${scheduleId}`).create({
        to,
        message: {
          subject: email.subject,
          text: email.text,
          attachments: [{
            filename: 'workout.ics',
            content: email.ics,
            contentType: 'text/calendar; charset=utf-8; method=PUBLISH',
          }],
        },
      });
    } catch (e) {
      if (e.code === 6) return;   // ALREADY_EXISTS: sent on an earlier try
      throw e;
    }
  },
);
