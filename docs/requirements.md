# Project charter and requirements

## Charter

**Project:** Teacher Tracker, SE 201, Fall 2026. **Purpose:** give teachers a clear way to submit and maintain an application, and give an administrator a small local tool for reviewing records and managing schools. An account-based waiting room supports a controlled lecture demonstration of admission and inactivity.

**Intended users:** teachers and the local administrator. These are proposed stakeholders from the specification, not people interviewed during implementation. **Topic approval:** supplied by the user. **Sponsor and team names:** to be supplied by the student team. **Budget:** no paid services are required. No labor estimate or measured hardware-capacity claim is made.

**Success criteria:** a reproducible Java website; persistent local records; server-enforced permissions; a functioning waiting room; at least five documented use cases and executed tests; consistent UML linked to code; genuine input/output evidence; and a report that distinguishes tested work from remaining coursework.

**Scope:** accounts, teacher applications, review feedback, schools, search/counts, configurable admission, documentation and tests. **Excluded:** workshops, emails, uploads, curriculum content, merging, payments, cloud databases, public hosting, desktop apps, distributed queues, CPU/RAM targets and complete DDoS protection.

## Functional requirements

| ID | Requirement | Main implementation |
|---|---|---|
| FR-01 | A visitor registers a teacher account with validated name, email, password, school and message. | PageController, Forms, TeacherService |
| FR-02 | Registration normalizes and uniquely stores email, hashes passwords and always assigns TEACHER with Pending status. | TeacherService, schema.sql |
| FR-03 | Accounts sign in with email/password and log out through CSRF-protected server sessions. | SecurityConfig |
| FR-04 | Pending, Approved and Rejected teachers can view only their own details, status and feedback after admission. | PageController dashboard |
| FR-05 | Teachers edit only their own name, email, school and message, retaining review status and feedback. | Profile DTO, principal ID |
| FR-06 | Administrators list, add, view, edit and delete teachers. Deletion removes their profile and temporary admission state. | AdminController, TeacherService |
| FR-07 | Administrators set Pending, Approved or Rejected status and feedback visible to the teacher. | TeacherService.review |
| FR-08 | Administrators search names/emails and filter by status, with teacher, school and pending counts. | TeacherService.search, admin-teachers |
| FR-09 | Administrators add, edit and delete unused schools. Referenced schools cannot be deleted. | SchoolService, database foreign key |
| FR-10 | Administrators save validated active capacity, inactivity timeout and bounded queue capacity, and observe current accounts. | AdmissionSettings, SettingsService |
| FR-11 | Distinct teacher accounts enter immediately when space is free or join a bounded FIFO queue without duplicates. | AdmissionService.enter |
| FR-12 | Waiting teachers see position and enter automatically after promotion. A full queue shows a busy/retry message. | waiting.js, admission/status |
| FR-13 | Logout and inactivity release places. Idle authentication survives while protected interactions obtain admission again. | AdmissionService, interceptor |
| FR-14 | Meaningful interactions renew activity. Polling, static files and merely open tabs do not. | activity.js, inspect/touch |
| FR-15 | Server checks prevent queued access to teacher URLs and reject expired form submissions without claiming a save. | AdmissionInterceptor |
| FR-16 | Administrators bypass admission limits. Login, logout, static assets and waiting checks remain reachable. | SecurityConfig, interceptor |
| FR-17 | Account/profile/school/review/settings records survive a restart; temporary admission state may reset. | File-backed H2 |

## Nonfunctional requirements

| ID | Requirement | Verification or limit |
|---|---|---|
| NFR-01 | Keep one understandable Java application with local assets and no mandatory external services. | Source and dependency inspection |
| NFR-02 | Hash passwords, enforce roles/ownership, validate inputs, keep CSRF enabled and escape output. | Integration tests and rendered pages |
| NFR-03 | Make admission decisions thread-safe and keep active and waiting state authoritative. | Synchronized service and concurrency test |
| NFR-04 | Keep waiting requests short, using lightweight checks rather than sleeping request threads. | Source review and actual handover |
| NFR-05 | Keep records on disk and exclude local data/cache files from source distribution. | Actual restart and archive inspection |
| NFR-06 | Support keyboard-friendly labelled forms and readable layouts at laptop/narrow widths. | Browser inspection; no formal accessibility audit |
| NFR-07 | Provide a reproducible build and macOS/Windows instructions. | Maven Wrapper, macOS build; Windows unverified |
| NFR-08 | Stay local by default, without public deployment or hardware-performance guarantees. | Loopback binding and stated scope |

## Risks and assumptions

One server process owns the local H2 database and in-memory admission state. A stable internal ID identifies each teacher. All example data is fictional. The administrator is trusted to manage records. There is no email recovery service. Browser scheduling and network delay can affect how soon a waiting page displays promotion; cleanup runs every second and checks run about every two seconds. Browser verification does not establish a security or load-testing certification.

## Course requirements checked against the attachment

The source deck is **Project Overview-fall2026-updated.pptx**, headed SE 201, Introduction to Software Engineering, Fall 2026. Slides 2–3 describe Property Management; the user's approved Teacher Tracker substitution is retained.

- Slide 4 asks for the Kickoff Project Charter and numbered functional/nonfunctional requirements. It gives October 10 for that phase.
- Slide 5 asks for the Agile course and Kickoff Stakeholder Vision Document. It gives November 9 for that phase.
- Slide 6 requires a use case diagram with at least five cases, descriptions for every case, two class diagrams from two selected scenarios and one sequence/activity model tied to one case. It explicitly requires UML consistency.
- Slide 7 requires executable code implementing one class diagram, including the attributes and methods shown.
- Slide 8 requires at least five tests and screenshots of all test inputs and outputs.
- Slide 9 states presentation on November 30 and final report on December 3. The slide does not repeat a year; Fall 2026 comes from the cover. It references a Moodle report template that was not supplied.
- Slide 10 says deliverables form one comprehensive final report and encourages office-hour feedback.

Course participation, PMI accounts/courses/badges, stakeholder interviews, team authorship and submission remain student responsibilities. No credentials were collected, no courses were taken, no messages were sent and no coursework was submitted. The report is an implementation report rather than a claim to use an unavailable template. The handoff explicitly excludes creating a final presentation deck unless separately requested.
