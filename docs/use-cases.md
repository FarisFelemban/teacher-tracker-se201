# Use case descriptions

See [the use case diagram](diagrams/use-cases.svg). UC-06 and UC-05 are the two selected class-model scenarios. UC-05 is also the sequence-model scenario.

| ID | Name | Primary actor |
|---|---|---|
| UC-01 | Register teacher account | Visitor |
| UC-02 | Sign in and out | Teacher or Administrator |
| UC-03 | View application status | Teacher |
| UC-04 | Edit own profile | Teacher |
| UC-05 | Review teacher application | Administrator |
| UC-06 | Manage schools | Administrator |
| UC-07 | Configure admission | Administrator |
| UC-08 | Wait for admission | Teacher |
| UC-09 | Manage teacher records | Administrator |

## UC-01: Register teacher account

**Goal:** create one teacher account/profile. **Precondition:** an available school exists. **Trigger:** visitor chooses Create teacher account. **Main flow:** enter name, email, school, message and password; server validates input and normalizes email; password is hashed; account/profile are created transactionally as TEACHER/Pending; visitor sees confirmation and can sign in. **Alternatives:** invalid fields, missing/removed school or duplicate case-normalized email produce errors; no partial account is committed. **Postcondition:** a valid teacher can authenticate; submitted role/status fields cannot grant privileges. **Requirements:** FR-01–02.

## UC-02: Sign in and out

**Goal:** establish/release a server session. **Precondition:** account exists. **Trigger:** submit login form. **Main flow:** enter email/password; Spring Security verifies the password hash; route teacher to protected area or administrator to overview; submit logout with CSRF token when finished; invalidate session and release any admission membership. **Alternatives:** wrong credentials show an error; a deleted account is refused on its next protected request. **Postcondition:** teacher status does not prevent authentication; logout frees the account's shared active/queue place. **Requirements:** FR-03, FR-13, FR-16.

## UC-03: View application status

**Goal:** read own details, status and feedback. **Precondition:** authenticated teacher. **Trigger:** open dashboard. **Main flow:** check account and admission by stable ID; show only this teacher's profile, school, status and feedback with escaped text. **Alternatives:** full capacity redirects to UC-08; missing profile returns not found; admin pages remain forbidden. **Postcondition:** intentional navigation renews admission. **Requirements:** FR-04, FR-15.

## UC-04: Edit own profile

**Goal:** keep personal/application details current. **Precondition:** authenticated and admitted teacher. **Trigger:** select Edit my profile. **Main flow:** view current values; change name/email/school/message; validate and save transactionally using principal ID; show confirmation with updated dashboard. **Alternatives:** duplicate email/invalid school returns errors; expired or queued POST returns 409 and a clear unsaved message; injected account IDs/roles/status/feedback do not bind to editable fields. **Postcondition:** review status and feedback are preserved, even when email changes. **Requirements:** FR-05, FR-14–15.

## UC-05: Review teacher application

**Goal:** communicate an application decision. **Precondition:** authenticated administrator; target teacher exists. **Trigger:** open the teacher record. **Main flow:** choose Pending, Approved or Rejected; enter feedback; submit CSRF-protected review; validate; TeacherService updates status/feedback; redirect with confirmation; teacher sees decision on a later dashboard view. **Alternatives:** malformed status/too-long feedback returns validation errors; teacher role receives 403; missing target receives 404. **Postcondition:** persisted decision belongs to the selected teacher. **Requirements:** FR-07.

## UC-06: Manage schools

**Goal:** maintain a small school list. **Precondition:** administrator session. **Trigger:** open Schools. **Main flow:** list schools; add a name/city; edit a selected school; delete an unused school when needed. SchoolService performs the database operations. **Alternatives:** invalid fields show errors; a referenced school cannot be deleted and displays an explanation; the foreign key protects against concurrent reference creation. **Postcondition:** saved changes persist and teachers keep valid school references. **Requirements:** FR-09, FR-17.

## UC-07: Configure admission

**Goal:** set capacity and inactivity for normal use or demonstration. **Precondition:** administrator session. **Trigger:** open Waiting room. **Main flow:** enter active limit, timeout and queue capacity; validate bounds; persist; clean/promote eligible waiters; show current active/waiting teachers. **Alternatives:** invalid values do not save; lowering active capacity keeps occupants until normal release; shortening timeout evaluates existing activity timestamps; lowering queue capacity retains current waiters. **Postcondition:** new rules apply to subsequent queue operations/cleanup and survive restart. **Requirements:** FR-10, FR-16–17.

## UC-08: Wait for admission

**Goal:** enter protected teacher area fairly. **Precondition:** authenticated teacher. **Trigger:** request teacher area while no place is free. **Main flow:** deduplicate by stable account ID; append to FIFO queue; show position; automatic checks retain waiting liveness without renewing active inactivity; cleanup/logout frees a place; promote first live waiter; browser opens dashboard. **Alternatives:** bounded queue is full, so show busy/retry; waiting checks stop for 15 seconds, so remove stale entry; inactive returning teacher joins behind existing waiters. **Postcondition:** one shared place per account, within capacity except permitted temporary over-capacity after lowering the setting. **Requirements:** FR-11–16.

## UC-09: Manage teacher records

**Goal:** maintain account/profile records. **Precondition:** administrator session. **Trigger:** open Teachers. **Main flow:** view counts; search/filter; open a record; edit details or add a Pending teacher with a locally chosen initial password; confirm and delete an account if needed. **Alternatives:** duplicate email or invalid inputs show errors; deleting a teacher removes profile and admission membership; old sessions are refused on their next protected request. **Postcondition:** persisted teacher data matches the operation. **Requirements:** FR-06, FR-08, FR-17.
