# Automated test results

Final clean package build: Maven Wrapper 3.9.11, Spring Boot 4.1.2, Oracle Java 26.0.1, macOS arm64. Executed October 29, 2026. Final clean build completed at 19:29 Asia/Riyadh.

## edu.se201.tracker.AdmissionServiceTest

Tests: 12; failures: 0; errors: 0; skipped: 0.

- PASS: staleWaitingAccountsAreRemovedBeforePromotion
- PASS: loweringQueueCapacityKeepsExistingWaitersAndBlocksNewOnes
- PASS: queueCapacityIsFiniteAndReleaseRemovesQueuedAccounts
- PASS: waitingPagePollingDoesNotKeepPromotedTeacherActiveForever
- PASS: meaningfulActivityExtendsAdmissionBeyondOriginalTimeout
- PASS: expiredTeacherReturnsBehindWaitingAccounts
- PASS: invalidSettingsFailBeforeStateChanges
- PASS: loweringCapacityDoesNotEjectCurrentTeachersAndRaisingPromotes
- PASS: tabsAndSessionsShareOnePlaceAndOneQueueEntry
- PASS: simultaneousRequestsNeverExceedCapacityOrDuplicateAccounts
- PASS: dashboardPollingDoesNotRenewActivityAndNextTeacherEnters
- PASS: firstAccountEntersAndNextAccountsWaitInArrivalOrder

## edu.se201.tracker.ApplicationIntegrationTest

Tests: 13; failures: 0; errors: 0; skipped: 0.

- PASS: successfulAndIncorrectLoginAreHandled
- PASS: registrationNormalizesEmailHashesPasswordAndIgnoresInjectedPrivileges
- PASS: duplicateEmailDuringEditRollsBackOtherProfileChanges
- PASS: adminCreatesEditsSearchesAndDeletesTeacherWhileSchoolIsProtected
- PASS: adminApprovesRejectsAndFeedbackAppearsEscapedForTeacher
- PASS: loginSchoolAndRegistrationPagesRenderWithoutAuthentication
- PASS: fullAreaEnforcesQueueOnDirectUrlsAndRejectsUnadmittedProfilePosts
- PASS: caseVariantDuplicateAndInvalidSchoolAreRejectedWithoutPartialAccounts
- PASS: schoolCanBeAddedEditedAndDeletedWhenUnused
- PASS: invalidSettingsDoNotSaveAndValidChangesPersist
- PASS: csrfRequiredForRegistrationReviewAndLogout
- PASS: teacherCannotAccessAdminAndOwnProfileCannotTargetAnotherAccount
- PASS: optionalDemoSeedingIsRepeatableWithoutResettingAccounts
