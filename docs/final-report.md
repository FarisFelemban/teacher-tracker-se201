# Teacher Tracker: final implementation report

Prepared October 29, 2026 (Asia/Riyadh) for SE 201, Introduction to Software Engineering, Fall 2026.

## Outcome

The project is an original Java website implementing the agreed small-to-medium Teacher Tracker. It includes teacher registration/login, private profile editing, application status/feedback, administrator management, schools, search/counts and a configurable waiting room. H2 saves records on the local computer. There is no public deployment or mandatory cloud service.

The final automated suite contains **25 passing tests, with no failures, errors or skipped tests**. Real browser checks exercised registration, duplicates, profile edits, reviews, administrator account creation, school management, admission, inactivity, continued editing, rejected expired saves and an actual application restart. Genuine screenshots accompany twelve browser cases. See [test cases](test-cases.md) and [individual automated results](automated-results.md).

The private source repository is [FarisFelemban/teacher-tracker-se201](https://github.com/FarisFelemban/teacher-tracker-se201). The editable source folder and source ZIP include code, tests, run instructions, requirements, UML, evidence and demonstration notes. Local database files, dependency caches, build output and development scripts are excluded from the source distribution.

## Scope delivered

Teachers register with name, email, password, school and a short application message. Email is normalized and unique, passwords use BCrypt, public registration assigns only TEACHER, and applications start Pending. Pending and Rejected teachers can still sign in. An admitted teacher can see/edit only their own details and read administrator feedback. Profile changes retain the application decision and feedback.

The administrator can view/search/filter teachers, read simple counts, add an account with a chosen initial demo password, edit/delete teachers, set Pending/Approved/Rejected status and feedback, manage schools, save admission settings and observe active/waiting accounts. Referenced schools cannot be deleted. Teacher deletion removes the profile and queue membership and causes existing sessions for that account to be refused on their next protected request.

The website has readable local styling, labelled forms, field/global errors, keyboard focus indicators and no external font or asset dependency. Its interface is intentionally modest. Responsive CSS is present, but a real narrow viewport could not be established in the available browser, so small-screen behavior is not claimed as verified.

Excluded features remain excluded: workshops, events, email services, uploads, protected curriculum, account merging, maps, analytics, payments, social login, MFA, recovery emails, organizations, distributed services, public hosting and resource-utilization targets. Password changes are optional in the handoff and are not implemented. This is not a desktop application or a complete translation of the Rails project.

## Technology and compatibility

| Component | Selected/resolved version | Purpose |
|---|---|---|
| Java source target | 21 | Uses standard records and collection APIs |
| Actual test/runtime JDK | Oracle 26.0.1, arm64 | Installed on the verification computer |
| Spring Boot | 4.1.2 | Single application and embedded server |
| Spring Framework / MVC / JDBC | 7.0.10 | Controllers, validation integration and explicit parameterized SQL |
| Spring Security | 7.1.2 | Session authentication, roles, BCrypt and CSRF |
| Thymeleaf | 3.1.5.RELEASE | Escaped server-rendered HTML |
| H2 | 2.4.240 | Local persistent database and isolated in-memory tests |
| Maven | 3.9.11 | Pinned by the included wrapper |
| Maven Wrapper plugin | 3.3.4 | Official shell and Windows scripts |
| JUnit / Spring test support | Managed by Spring Boot 4.1.2 | Unit and MVC/security integration tests |
| PlantUML | 1.2026.9 | Editable/rendered course diagrams only |

Versions were resolved from Maven Central and checked by a real clean build. [Spring Boot's official requirements](https://docs.spring.io/spring-boot/system-requirements.html) list Java support through 27 for this release and Maven 3.6.3 or later. Java 26 is therefore supported by the selected release. The Maven distribution checksum is pinned in `.mvn/wrapper/maven-wrapper.properties`.

The supplied stack did not require a particular persistence API. Spring JDBC was chosen for concise, explicit SQL rather than introducing ORM entity mappings. This is a small implementation choice within the single Spring Boot/H2 design, not a change to the backend language, authentication method or database location.

## Application structure

`TrackerApplication` boots the app and supplies the UTC clock. `SecurityConfig` loads accounts and configures authentication, roles, CSRF and logout. `PageController` handles public/teacher flows. `AdminController` handles administrator flows. `Forms` contains validated request objects, separate from account privileges and review fields.

`TeacherService` manages account/profile transactions. `SchoolService` manages schools and protected deletion. `SettingsService` persists admission settings. `AdmissionService` owns the temporary account-based active/queue maps and uses a controllable Clock. `AdmissionInterceptor` enforces admission on every protected teacher request and refuses deleted accounts. `SeedData` creates initial schools/admin without resetting existing data and optionally creates fictional teachers only when no teacher profiles exist.

Templates, CSS and the two small JavaScript files are under `src/main/resources`. Tests are under `src/test`. There is no React/Node frontend, microservice, Redis, broker, Docker requirement or external authentication service.

## Database structure and persistence

| Table | Main fields and constraints |
|---|---|
| user_account | Stable ID, normalized unique email, password_hash, TEACHER/ADMIN role |
| teacher_profile | Account ID as primary/foreign key, name, school foreign key, application message, status, feedback |
| school | ID, name, city |
| admission_settings | Single settings row, bounded active capacity, inactivity seconds and queue capacity |

One profile belongs to one teacher account. The teacher view's email is joined from user_account, so there are not two persisted email sources. Profile ownership and admission use account ID rather than email. Deleting an account cascades to its profile. A school foreign key prevents orphaned teacher references, including a concurrent reference created around a delete attempt. Registration and related account/profile changes use transactions and database constraints.

The normal JDBC URL is `jdbc:h2:file:./data/teacher-tracker;DB_CLOSE_ON_EXIT=FALSE`. It stores data on the host laptop, not in browser localStorage. Separate browsers share the same server/database. Always run from the same folder or configure an absolute data path. Login sessions and the waiting room reset on restart; persisted records/settings remain.

The real restart check confirmed Amina's changed name/message, school association, Approved status, feedback and the saved admission settings. A later restart additionally checked the edited fictional school and restored default settings. Local runtime data is deliberately not part of Git or the source ZIP.

## Authentication, permissions and validation

Spring Security compares BCrypt password hashes and creates ordinary server sessions. The principal stores a stable account ID. Email changes affect future login names while the current authenticated session still identifies the same account. Hashes are not rendered in tables/forms or API responses. Public forms have no writable role/status/feedback fields. Teacher profile updates use the principal's ID and ignore submitted target IDs.

Administrator routes require ADMIN on the server. Teacher routes require TEACHER. CSRF remains enabled for registration, profile/review/settings submissions and logout. Input lengths, email format, valid school selection, passwords and settings are validated. Passwords are limited to 10–64 ASCII characters so they stay within BCrypt's input-length constraints. Duplicate case-normalized emails are rejected transactionally. Thymeleaf escapes names, messages and feedback. JDBC uses bound parameters. The H2 console is disabled.

These are proportionate course-project controls. There is no claim of a full security audit, public-internet hardening or complete DDoS defense. The application binds to 127.0.0.1 by default and all documented credentials are fictional local-demo credentials.

## Waiting room and inactivity

Admission is separate from application approval. It limits distinct teacher accounts in the protected teacher area. Pending and Rejected teachers use the same rules as Approved teachers; administrators bypass the limit.

One synchronized AdmissionService owns all active/waiting state. Active IDs map to last meaningful activity times. Waiting IDs stay in insertion order and map to last waiting-page checks. Cleanup expires inactive active places and stale waiting entries, then promotes live waiters before considering a newcomer. Multiple sessions/tabs for one account share one place and never duplicate a queue entry. Request threads return promptly while browser checks perform the waiting.

**Activity definition:** intentional GET navigation in the teacher area and admitted form submissions renew activity. The teacher page also sends a CSRF-protected signal after trusted input, keyboard or click events, at most once every five seconds. Mouse movement, background status checks, static-file requests and leaving a tab open do not count. There is no unconditional heartbeat.

**Expired state:** the authenticated session remains. The old page displays an expiry message. A later teacher navigation requests admission again and joins behind existing waiters. A profile POST without a current place returns 409 and an explicit not-saved page, even if a new place could now be obtained. It does not silently save or report success.

**Automatic entry:** waiting pages check about every two seconds, keep their waiting entry alive and redirect to the dashboard after promotion. Polling an active dashboard uses inspect and does not renew inactivity. Waiting checks after promotion also do not indefinitely hold a place. A waiting account that stops checking expires after 15 seconds. A full queue shows a busy/retry message without blocking a server thread.

**Settings:** defaults are 20 active accounts, 300 seconds and 100 waiting accounts. Active/queue capacities allow 1–1000 and timeout allows 10–3600 seconds. Ten seconds is a documented lower bound to keep the five-second interaction signal coherent; clock-based tests use short logical time jumps without waiting minutes. Settings persist.

Lowering active capacity preserves existing occupants and temporarily permits the active count to remain above the new limit. No new account enters until the count falls below the limit. Increasing capacity promotes eligible waiters. Lowering queue capacity preserves current waiters and refuses new entries until space exists. Shortening timeout evaluates existing timestamps under the new duration at the next cleanup, so an already idle teacher may expire immediately. Queue state is in memory and resets on restart.

## Build, run and demo accounts

Full [macOS/Windows instructions](../README.md) are included. On macOS, from the project folder:

```sh
./mvnw clean package
java -jar target/teacher-tracker-1.0.0.jar --tracker.demo-teachers=true
```

On Windows, use `.\mvnw.cmd clean package`, followed by the same Java command. Open `http://127.0.0.1:8080`. Stop the application before rebuilding its running JAR. The first build requires internet; normal use of a built application does not require a cloud service.

| Demo role | Email | Fictional password |
|---|---|---|
| Administrator | admin@demo.test | AdminDemo2026! |
| Teacher A | amina@demo.test | TeacherDemo2026! |
| Teacher B | bilal@demo.test | TeacherDemo2026! |

The administrator is created automatically on a new database. The optional flag adds teachers only if there are no teacher profiles. Existing accounts and passwords are not reset. All three credentials are examples for the local demonstration, not personal accounts. Additional teacher accounts can be created through registration/admin forms. There is no email invitation/reset infrastructure.

## Verification and evidence

The unit tests use a mutable clock to check FIFO, capacity, duplicate sessions, activity, observation-only polling, stale waiters, promotion, bounded queues and configuration changes. A 100-request concurrent test over 50 distinct IDs confirms three active places and 47 unique queued accounts when the limit is three.

The integration tests load the actual Spring application with an isolated H2 test database. They exercise MVC validation, real form authentication, server role/ownership checks, CSRF, escaped feedback, admin CRUD, transactions, school constraints, queue enforcement and settings. They also verify optional demo seeding preserves edited email/name and existing administrator credentials.

The browser evidence contains twelve documented cases and input/output screenshots. The 25-second inactivity demonstration showed B at position 1, then automatic dashboard entry while A's idle page showed expiry. A separate editing trial lasted 27 seconds with meaningful keys about every nine seconds; A retained admission and B stayed waiting. The expired-save page explicitly said changes were not saved, and the later dashboard retained the earlier saved message. A real process restart established file persistence.

![Teacher dashboard after approval](evidence/TC04-teacher-approved.jpg)

![Waiting-room position](evidence/TC06-bilal-waiting.jpg)

![Expired save rejected](evidence/TC12-expired-profile-result.jpg)

The administrator's active-name display was corrected after a browser check revealed blank labels, and a corresponding integration assertion was added. A development rebuild of the still-running JAR interrupted a test session; restarting the app resolved it. The final workflow stops the app before rebuilding. Some Java 26 dependency/test tooling emits reflection/agent warnings, but the clean build and test suite complete successfully.

## Coursework mapping

| Course expectation | Delivered support |
|---|---|
| Charter, functional/nonfunctional requirements | [Requirements/charter](requirements.md), FR-01–17 and NFR-01–08 |
| Agile/stakeholder vision | [Vision and prioritized backlog](vision-and-agile.md), clearly marked as a proposed draft |
| At least five use cases and descriptions | Nine-case UML diagram and [nine descriptions](use-cases.md) |
| Two scenario class diagrams | UC-06 schools and UC-05 application review |
| One sequence/activity diagram for one use case | UC-05 review sequence with consistent participant/class names |
| Executable code for one complete selected class model | School and SchoolService, with every shown field/constructor/method mapped in [uml.md](uml.md) |
| At least five tests and actual input/output screenshots | 25 automated tests plus twelve documented browser cases with genuine captures |
| Final report/presentation | This implementation report and [lecture demonstration script](demo-script.md); final slide deck is outside this handoff |

The source slides are Fall 2026 coursework material. They give November 30 for the presentation and December 3 for the final report, without repeating a year on that slide. They refer to Moodle/PMI templates that were not provided. This report does not pretend to follow those templates or claim that PMI courses, badges, stakeholder interviews, office-hour feedback, presentation delivery or submission occurred. The student team still needs to add its real details and complete any remaining course obligations.

## Device, portability and limitations

Verification used **Apple M4, 16 GB RAM, macOS 26.6.2, Oracle JDK 26.0.1**. It did not use the requested M3 model, so M3 operation is a portability expectation rather than a measured result. Windows commands were reviewed but not run on a Windows device. The normal laptop-width browser pages were visually inspected. A narrow viewport override did not actually alter the browser width, so narrow-screen behavior remains unverified despite responsive CSS.

One server process and one local database are assumed. There is no distributed admission, migration framework, audit/history subsystem, password change/recovery, pagination, external email or performance guarantee. Default local-demo credentials suit the course environment only. Restarting clears authentication and admission state; losing a waiting place on restart is expected. Automatic display timing depends on cleanup, browser scheduling and connection delays. A long-idle protected form can be rejected and must be copied/re-entered after admission, as the UI explains.

No final slide deck, PMI badge, instructor-template completion or coursework submission is claimed. No messages were sent to third parties and no public application deployment was performed. The source is saved privately on GitHub at the user's request.
