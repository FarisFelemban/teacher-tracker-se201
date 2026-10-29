# Stakeholder vision and Agile approach

## Proposed stakeholder vision

**For** teachers who need to apply and maintain school details, **Teacher Tracker** is a local website that shows application progress and administrator feedback. **For** an administrator, it supplies a searchable teacher list, review controls and school management. The waiting room provides a small, visible admission-control example during the lecture.

Teachers need private ownership of their profiles, clear validation and a way to remain signed in after a waiting-room timeout. Administrators need accurate reviews, manageable school records and understandable admission settings. The student team needs readable code, reproducible tests and UML that matches implementation. The instructor needs evidence supporting the modeling, coding and testing requirements.

These needs come from the agreed handoff. No stakeholder interviews, PMI course completion or badge verification occurred. This is an original draft vision, not the unavailable PMI Stakeholder Vision template.

## Prioritized backlog

| Priority | Story | Acceptance condition |
|---|---|---|
| Must | As a teacher, I can register and sign in. | A valid account starts Pending; duplicates and invalid data are rejected. |
| Must | As a teacher, I can see/edit only my profile. | Crafted requests cannot change another account or assign status/role. |
| Must | As an administrator, I can review teacher applications. | Status and feedback appear on the correct teacher dashboard. |
| Must | As an administrator, I can maintain schools and records. | CRUD works and referenced schools remain protected. |
| Must | As a waiting teacher, I get a fair place. | FIFO, one place per account, bounded size, automatic promotion. |
| Must | As an active teacher, I retain my place while working. | Meaningful actions renew inactivity; polling does not. |
| Must | As a student, I can reproduce and explain the project. | Build instructions, UML mapping, tests and screenshots are provided. |
| Could | Password change or a richer visual design. | Deferred; not needed for the agreed course demonstration. |

## Iterative implementation

1. Establish the requirements and inspect the course/reference materials.
2. Implement accounts, profile ownership and persistent data.
3. Implement administrator review, schools, search and counts.
4. Integrate admission and inactivity, then test boundary/concurrency rules.
5. Run browser scenarios, capture evidence, fix observed issues and prepare documentation.

The source, automated test suite and saved evidence reflect these increments. This is an implementation workflow, not evidence that a multi-person Scrum team held meetings, used a particular sprint length or completed stakeholder sign-off.

**Definition of done:** requirements implemented or explicitly limited, build succeeds, relevant automated tests pass, important browser workflows are exercised, screenshots are genuine, UML agrees with code and remaining course work is disclosed.
