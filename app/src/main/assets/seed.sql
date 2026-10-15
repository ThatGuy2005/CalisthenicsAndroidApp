-- Premade content, loaded once when the database is first created (see SQLManager).
-- Muscle groups: Chest, Back, Legs, Glutes, Core, Shoulders, Arms, Full Body
-- Levels / difficulty: Beginner, Intermediate, Advanced
-- Workout categories: Full body, Upper body, Lower body, Core, Cardio
-- MET values follow the Compendium of Physical Activities calisthenics categories
-- (approximate): 3.5 light, 3.8 moderate, 8.0 vigorous. They only feed the kcal estimate.
-- AnimationFile is NULL (placeholder) until the animation files are added.
-- Timed exercises use DefaultSeconds, rep-based ones use DefaultReps; the other is NULL.
-- IsPerSide = 1: the reps or seconds are per side (each leg or arm), so a set does both sides.

-- ========== EXERCISES ==========
INSERT INTO Exercise
    (Name, Instructions, IsTimed, DefaultSeconds, DefaultReps, Met, AnimationFile, MuscleGroup, Difficulty,
     IsPerSide)
VALUES
('Jumping Jacks',
 '1) Stand upright with feet together and arms at your sides. 2) Jump your feet out wide while raising your arms overhead. 3) Jump back to the start and repeat at a steady rhythm. Tip: land softly on the balls of your feet.',
 1, 30, NULL, 8.0, NULL, 'Full Body', 'Beginner', 0),
('High Knees',
 '1) Stand tall with feet hip-width apart. 2) Run in place, driving each knee up to hip height. 3) Pump your arms and stay on the balls of your feet. Tip: keep your chest up and your core tight.',
 1, 30, NULL, 8.0, NULL, 'Full Body', 'Beginner', 0),
('Bodyweight Squat',
 '1) Stand with feet shoulder-width apart, toes slightly turned out. 2) Push your hips back and bend your knees until your thighs are parallel to the floor. 3) Press through your heels to stand up. Tip: keep your chest up and your knees in line with your toes.',
 0, NULL, 15, 3.8, NULL, 'Legs', 'Beginner', 0),
('Glute Bridge',
 '1) Lie on your back with knees bent and feet flat, hip-width apart. 2) Press through your heels and lift your hips until your body forms a straight line from shoulders to knees. 3) Squeeze your glutes at the top, then lower slowly. Tip: do not arch your lower back.',
 0, NULL, 15, 3.5, NULL, 'Glutes', 'Beginner', 0),
('Wall Sit',
 '1) Stand with your back against a wall and walk your feet forward. 2) Slide down until your thighs are parallel to the floor and your knees are at 90 degrees. 3) Hold the position with your back flat against the wall. Tip: keep your weight in your heels.',
 1, 30, NULL, 3.8, NULL, 'Legs', 'Beginner', 0),
('Knee Push-Up',
 '1) Start on hands and knees, with hands slightly wider than your shoulders. 2) Keep a straight line from knees to head and lower your chest toward the floor. 3) Push back up to the start. Tip: keep your elbows at about 45 degrees from your body.',
 0, NULL, 10, 3.8, NULL, 'Chest', 'Beginner', 0),
('Plank',
 '1) Rest on your forearms and toes with your elbows under your shoulders. 2) Keep your body in a straight line from head to heels. 3) Tighten your core and glutes and hold. Tip: do not let your hips sag or rise.',
 1, 30, NULL, 3.8, NULL, 'Core', 'Beginner', 0),
('Crunches',
 '1) Lie on your back with knees bent and feet flat, hands beside your head. 2) Curl your shoulders off the floor by contracting your abs. 3) Lower with control. Tip: do not pull on your neck.',
 0, NULL, 15, 3.8, NULL, 'Core', 'Beginner', 0),
('Superman Hold',
 '1) Lie face down with your arms extended in front of you. 2) Lift your arms, chest and legs a few centimeters off the floor. 3) Hold while squeezing your back and glutes. Tip: keep your neck neutral and look at the floor.',
 1, 20, NULL, 3.5, NULL, 'Back', 'Beginner', 0),
('Push-Up',
 '1) Start in a high plank with hands slightly wider than your shoulders. 2) Lower your chest to just above the floor, keeping your body in a straight line. 3) Push back up to the start. Tip: keep your core tight and your elbows at about 45 degrees.',
 0, NULL, 12, 3.8, NULL, 'Chest', 'Intermediate', 0),
('Lunges',
 '1) Stand tall with feet hip-width apart. 2) Step forward and lower until both knees are bent at about 90 degrees. 3) Push through your front heel to return, then alternate legs. Tip: keep your front knee above your ankle.',
 0, NULL, 12, 3.8, NULL, 'Legs', 'Intermediate', 1),
('Diamond Push-Up',
 '1) Start in a high plank with your hands together under your chest, thumbs and index fingers forming a diamond. 2) Lower your chest toward your hands. 3) Push back up. Tip: keep your elbows close to your body.',
 0, NULL, 8, 3.8, NULL, 'Arms', 'Intermediate', 0),
('Pike Push-Up',
 '1) Start in a downward-dog position with hips high and hands shoulder-width apart. 2) Bend your elbows and lower the top of your head toward the floor. 3) Press back up. Tip: keep your legs as straight as you can.',
 0, NULL, 8, 3.8, NULL, 'Shoulders', 'Intermediate', 0),
('Bench Dips',
 '1) Sit on the edge of a sturdy bench or chair and place your hands beside your hips. 2) Slide your hips off the edge and lower your body by bending your elbows to about 90 degrees. 3) Press back up. Tip: keep your back close to the bench.',
 0, NULL, 12, 3.8, NULL, 'Arms', 'Intermediate', 0),
('Leg Raises',
 '1) Lie on your back with legs straight and hands under your hips. 2) Lift your legs to vertical while keeping your lower back on the floor. 3) Lower slowly without letting your feet touch the floor. Tip: bend your knees slightly if your lower back lifts.',
 0, NULL, 12, 3.8, NULL, 'Core', 'Intermediate', 0),
('Bicycle Crunches',
 '1) Lie on your back with hands behind your head and legs lifted. 2) Bring one elbow toward the opposite knee while extending the other leg. 3) Switch sides in a smooth pedaling motion. Tip: rotate from your torso, not just your elbows.',
 0, NULL, 20, 3.8, NULL, 'Core', 'Intermediate', 0),
('Mountain Climbers',
 '1) Start in a high plank with hands under your shoulders. 2) Drive one knee toward your chest, then switch legs quickly. 3) Keep your hips low and your core tight. Tip: keep your shoulders above your wrists.',
 1, 30, NULL, 8.0, NULL, 'Core', 'Intermediate', 0),
('Burpees',
 '1) Stand, then squat and place your hands on the floor. 2) Jump or step your feet back into a plank and do a push-up if you like. 3) Jump your feet forward and explode upward into a jump. Tip: keep a steady pace and land softly.',
 0, NULL, 10, 8.0, NULL, 'Full Body', 'Intermediate', 0),
('Pull-Up',
 '1) Hang from a bar with an overhand grip slightly wider than your shoulders. 2) Pull your chest toward the bar by driving your elbows down. 3) Lower with control to a full hang. Tip: avoid swinging or kicking.',
 0, NULL, 8, 3.8, NULL, 'Back', 'Advanced', 0),
('Dips',
 '1) Support yourself on parallel bars with your arms straight. 2) Lean slightly forward and lower until your elbows are at about 90 degrees. 3) Press back up to straight arms. Tip: keep your shoulders away from your ears.',
 0, NULL, 10, 3.8, NULL, 'Chest', 'Advanced', 0),
('Pistol Squat',
 '1) Stand on one leg with the other leg extended in front of you. 2) Lower into a deep single-leg squat while keeping your heel on the floor. 3) Drive through your heel to stand up. Tip: hold a doorframe for balance while you build strength.',
 0, NULL, 5, 3.8, NULL, 'Legs', 'Advanced', 1),
('L-Sit Hold',
 '1) Sit between two parallel bars, or on the floor with your hands beside your hips. 2) Press down to lift your body and extend your legs straight in front of you. 3) Hold with locked arms and a tight core. Tip: bend your knees if you cannot keep your legs straight.',
 1, 15, NULL, 3.8, NULL, 'Core', 'Advanced', 0),
('Archer Push-Up',
 '1) Start in a wide push-up position. 2) Lower toward one hand while keeping the opposite arm straight. 3) Push back up and alternate sides. Tip: master standard push-ups first.',
 0, NULL, 6, 3.8, NULL, 'Chest', 'Advanced', 1);

-- ========== PREMADE WORKOUTS ==========
INSERT INTO Workout (Name, Level, Category) VALUES
('Beginner Full Body', 'Beginner', 'Full body'),
('Core Basics', 'Beginner', 'Core'),
('Upper Body Strength', 'Intermediate', 'Upper body'),
('Lower Body Burn', 'Intermediate', 'Lower body'),
('Cardio Blast', 'Intermediate', 'Cardio'),
('Advanced Calisthenics', 'Advanced', 'Full body');

-- ========== EXERCISES IN EACH WORKOUT ==========
-- Reps / Seconds NULL = use the exercise default. RestSeconds is the rest between the sets of an
-- exercise, RestAfterSeconds the rest after its last set, before the next exercise.
INSERT INTO ExerciseOrder
    (WorkoutName, ExerciseName, Position, Sets, Reps, Seconds, RestSeconds, RestAfterSeconds)
VALUES
('Beginner Full Body', 'Jumping Jacks',     1, 3, NULL, NULL, 30, 60),
('Beginner Full Body', 'Bodyweight Squat',  2, 3, NULL, NULL, 45, 60),
('Beginner Full Body', 'Knee Push-Up',      3, 3, 8,    NULL, 45, 60),
('Beginner Full Body', 'Glute Bridge',      4, 3, NULL, NULL, 45, 60),
('Beginner Full Body', 'Plank',             5, 3, NULL, 20,   45, 60),

('Core Basics', 'Crunches',        1, 3, NULL, NULL, 30, 60),
('Core Basics', 'Plank',           2, 3, NULL, NULL, 30, 60),
('Core Basics', 'Superman Hold',   3, 3, NULL, NULL, 30, 60),
('Core Basics', 'Glute Bridge',    4, 3, NULL, NULL, 30, 60),

('Upper Body Strength', 'Push-Up',          1, 4, NULL, NULL, 60, 90),
('Upper Body Strength', 'Diamond Push-Up',  2, 3, NULL, NULL, 60, 90),
('Upper Body Strength', 'Pike Push-Up',     3, 3, NULL, NULL, 60, 90),
('Upper Body Strength', 'Bench Dips',       4, 3, NULL, NULL, 60, 90),

('Lower Body Burn', 'Bodyweight Squat', 1, 4, NULL, NULL, 45, 60),
('Lower Body Burn', 'Lunges',           2, 3, NULL, NULL, 45, 60),
('Lower Body Burn', 'Wall Sit',         3, 3, NULL, NULL, 45, 60),
('Lower Body Burn', 'Glute Bridge',     4, 3, 20,   NULL, 30, 60),

('Cardio Blast', 'Jumping Jacks',     1, 3, NULL, NULL, 20, 60),
('Cardio Blast', 'High Knees',        2, 3, NULL, NULL, 20, 60),
('Cardio Blast', 'Mountain Climbers', 3, 3, NULL, NULL, 20, 60),
('Cardio Blast', 'Burpees',           4, 3, 8,    NULL, 40, 60),

('Advanced Calisthenics', 'Pull-Up',         1, 4, NULL, NULL, 90, 120),
('Advanced Calisthenics', 'Dips',            2, 4, NULL, NULL, 90, 120),
('Advanced Calisthenics', 'Archer Push-Up',  3, 3, NULL, NULL, 90, 120),
('Advanced Calisthenics', 'Pistol Squat',    4, 3, NULL, NULL, 90, 120),
('Advanced Calisthenics', 'L-Sit Hold',      5, 3, NULL, NULL, 60, 120);
