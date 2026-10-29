# Lecture demonstration

## Before the lecture

1. Build once while internet is available: `./mvnw clean package` (Windows: `.\mvnw.cmd clean package`).
2. Start from the same project folder with `java -jar target/teacher-tracker-1.0.0.jar --tracker.demo-teachers=true`.
3. Have three isolated sessions ready: administrator, Teacher A and Teacher B. Use separate browser profiles/browsers. Two normal tabs sharing the same host share login cookies. For local verification here, 127.0.0.1 and localhost supplied different host cookies; both reached the same database. A third browser/profile is still useful for the administrator.
4. Use only fictional accounts listed in the README. Confirm that no school/teacher names entered for the demonstration are personal data.

## Five-to-eight-minute demonstration

1. Sign in as administrator. Show teacher/school/pending counts and the teacher list.
2. Open Amina's record. Show the school and application message, choose Approved and save feedback. Show Amina's dashboard in her isolated session.
3. Edit Amina's message and save. Explain that ordinary profile edits retain review status.
4. Open administrator Waiting room settings. Set maximum active teachers to **1**, timeout to **25 seconds**, queue capacity to **10**. Explain that the low limit is deliberate; it is not a measured laptop limit.
5. Open Teacher A's dashboard, then sign in as Teacher B in another isolated session. B should see queue position 1.
6. For more than 25 seconds, navigate in A's teacher area or type in the profile form at intervals below the timeout. Show that B remains waiting. Typing/clicking sends a signal at most every five seconds.
7. Stop touching A. Keep B's waiting page open. After 25 seconds from A's last meaningful activity, plus cleanup/check delay, B enters automatically. A's old page displays an expiry message, while A remains signed in.
8. Open A's dashboard again. While B is active, A joins behind any existing waiters. Do not claim that A's old profile submission saved if the admission check rejects it.
9. Increase active capacity to **2**. Both accounts can now enter. The administrator never consumes a teacher place.
10. Show Schools. Attempt to delete a referenced school, then show the explanation. Add/edit/delete a fictional unused school if time permits.
11. Save a recognizable message and settings. Stop with Ctrl+C and restart using the same command/folder. Sign in again and show that accounts, message, school, review and settings remain. Explain that logins and the temporary queue reset.
12. Restore defaults of **20 active**, **300 seconds**, **100 waiting** after the demonstration.

## Points to explain

The backend is Java, the browser renders server templates, and H2 writes data to a local file. Passwords are hashes. Role and ownership checks happen on the server. Admission counts accounts, not browser tabs or Java server instances. Polling observes status without keeping idle dashboards active. The project uses no live attack, resource-utilization target or public hosting.

## Remaining presentation work

These notes support the course presentation but are not a final slide deck. The student team should add its names, describe the real teamwork/course activities it completed and fit the content to the instructor's presentation/report template when supplied.
