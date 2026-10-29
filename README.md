# Teacher Tracker

A local browser website for the SE 201 course. Teachers apply, sign in, edit their own profiles and read application feedback. An administrator manages teachers, schools and an account-based waiting room.

## Requirements

Install a supported JDK, version 21 or later compatible with Spring Boot 4.1.2 (up to Java 27). This project was built and tested on macOS arm64 with Oracle JDK 26.0.1. Java 21 is the compilation target. Windows instructions are provided but have not been executed on Windows.

You do not need to install Maven or a database server. The included Maven Wrapper downloads Maven 3.9.11. The first build needs internet access. Dependencies come from Maven Central. Normal application use needs no cloud connection.

## Start on macOS

Open Terminal in this project folder:

```sh
chmod +x mvnw
./mvnw clean package
java -jar target/teacher-tracker-1.0.0.jar --tracker.demo-teachers=true
```

Open **http://127.0.0.1:8080**. Stop with Control+C in Terminal. Stop the application before rebuilding its JAR file.

## Start on Windows

Open PowerShell in this project folder:

```powershell
.\mvnw.cmd clean package
java -jar target/teacher-tracker-1.0.0.jar --tracker.demo-teachers=true
```

Open **http://127.0.0.1:8080**. Stop with Ctrl+C. A JDK must be installed and its `java` command must be on PATH. If the wrapper cannot find Java, set JAVA_HOME to your JDK folder.

For another port, append `--server.port=8081` to the Java command. The default server binds only to 127.0.0.1, so it is accessible from the local computer.

## Fictional demo accounts

| Role | Email | Password | Availability |
|---|---|---|---|
| Administrator | admin@demo.test | AdminDemo2026! | Created on first startup |
| Teacher A | amina@demo.test | TeacherDemo2026! | Created with the demo flag if no teacher profiles exist |
| Teacher B | bilal@demo.test | TeacherDemo2026! | Created with the demo flag if no teacher profiles exist |

All accounts and schools are fictional. Existing accounts, passwords, profiles, reviews and settings are preserved on subsequent starts. Optional demo teachers start Pending and are seeded only when there are no teacher profiles, so editing a demo email does not cause duplicate accounts on restart. Startup without the demo flag creates only the administrator and initial schools on a fresh database.

To create another teacher, use public registration or the administrator's **Add teacher** form. The administrator chooses an initial password of 10–64 ASCII characters and shares it verbally during the local demonstration. This project does not include password changes, email delivery or password recovery.

## Database and persistence

The default database is `data/teacher-tracker.mv.db`, relative to the folder where the Java command runs. Always start from the same project folder to reuse it. Accounts, schools, profiles, reviews and settings survive a restart. Waiting-room state and login sessions reset.

To choose a different data folder, supply an absolute path with `--spring.datasource.url=jdbc:h2:file:/absolute/path/teacher-tracker;DB_CLOSE_ON_EXIT=FALSE` as one quoted argument, or set the `TRACKER_DATA_DIR` environment variable. Stop the application before making a backup of the data folder. Database files are excluded from Git and the source ZIP.

## Tests

```sh
./mvnw test
```

On Windows use `.\mvnw.cmd test`. Tests use an isolated in-memory H2 database. See [test cases](docs/test-cases.md) for the recorded automated results and genuine browser screenshots.

## Waiting-room rules

The administrator sets maximum active teachers, inactivity timeout and queue capacity. Defaults are **20 active**, **300 seconds**, **100 waiting**. All settings persist. The timeout range is 10–3600 seconds, and each capacity range is 1–1000. The ten-second minimum keeps the five-second interaction throttle useful.

Each teacher account receives one place across its tabs and sessions. The queue uses arrival order. Administrators bypass it. A waiting page checks every two seconds and automatically opens the dashboard after admission. Stale waiting entries expire after 15 seconds without checks. A full queue shows a busy message and retries.

Navigation and form submissions count as activity. Actual typing, keyboard use and clicks on teacher pages send an activity signal at most once every five seconds. Mouse movement, background checks, static assets and an open idle tab do not renew activity. After inactivity, the login remains valid, but the next protected action needs admission again. An expired profile submission returns HTTP 409 and explicitly says changes were **not saved**.

Lowering the active limit keeps current teachers until they leave or become inactive. New admissions wait until the count falls below the limit. Lowering queue capacity keeps existing waiters and blocks new entries while the queue is over its new limit. A shorter timeout applies to existing last-activity times at the next cleanup. Restarting clears the temporary queue.

This feature is a local admission-control demonstration. It makes no DDoS-protection or hardware-capacity guarantee.

## Project guide

- [Final implementation report](docs/final-report.md)
- [Project charter and requirements](docs/requirements.md)
- [Stakeholder vision and Agile approach](docs/vision-and-agile.md)
- [Use cases](docs/use-cases.md)
- [UML models and code mapping](docs/uml.md)
- [Test cases and screenshots](docs/test-cases.md)
- [Lecture demonstration script](docs/demo-script.md)

Code is in `src/main/java/edu/se201/tracker`. Pages and local CSS/JavaScript are in `src/main/resources`. The application uses Spring MVC, Security, Thymeleaf and Spring JDBC. JDBC keeps the database operations explicit and uses parameterized queries. It adds no separate database installation or service.

## Reference and coursework

The idea is based on [BJC Teacher Tracker](https://github.com/cs169/BJC-Teacher-Tracker). Its README, Rails routes, schema, teacher model and controller informed the scope. This Java implementation and its styling are original. No reference code, logos, CSS, templates or images were copied.

The supplied course slides are a requirements source. The Teacher Tracker topic approval is user-provided. The instructor's report template was not supplied. These documents support the coursework but do not claim completion of PMI courses, badges, stakeholder interviews, account verification, a final slide deck or submission.
