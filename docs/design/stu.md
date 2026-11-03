# Students (STU) Design

> Realizes: UC-STU-deactivate-student
> Depends on: FR-SEC-active-account, BR-student-lifecycle, BR-submission-owed (a deactivated student is owed nothing and is not evaluated; see [not.md](not.md) and UC-EVA-submit-evaluation)
> See: [use cases](../requirements/use-cases.md#student), [SRS security requirements](../requirements/software-requirements-specification.md), [architectural-design.md](architectural-design.md#shared-foundation-components) (shared foundation component view, Crosscutting Concepts › Security & Compliance)
> Designed against `main` at `59f0267` (2026-11-03).

## Overview

This area covers the student lifecycle in the `student` foundation slice: who may change a student's account state, and how that state takes effect. Deactivation is a **state change on the existing `enabled` flag** of `PeerEvaluationUser`. It is not a removal: team membership, weekly activity reports, and peer evaluations are left alone, so the change can be reversed (UC-STU-reactivate-student). Because she stays on her team, each consumer of a team roster has to tell an active member from a deactivated one (Key decisions 5–7).

Before this design, the only way to deactivate a student was the `enabled` field of the generic `PUT /students/{studentId}` edit. That route also admits the student themselves, the actor was never warned about the consequences, and the flag was checked only at login. A deactivated student's existing JWT therefore kept working for up to its 2-hour lifetime, in breach of FR-SEC-active-account.

## Components & classes

```mermaid
classDiagram
    direction LR
    class StudentController {
        <<reused, student>>
        +deactivateStudent(studentId) Result  «new»
    }
    class StudentService {
        <<reused, student>>
        +deactivateStudent(studentId) Student  «new»
    }
    class StudentSecurityService {
        <<reused, student>>
        +isCurrentUserInstructorOfStudentSection(studentId) boolean
        +isCurrentUserAdminOfStudentCourse(studentId) boolean  «new»
    }
    class StudentLifecycleAuthorizationManager {
        <<new, security/authorizationmanagers>>
        +authorize(auth, context) AuthorizationResult
    }
    class ActiveAccountJwtAuthenticationConverter {
        <<new, security>>
        +convert(Jwt) AbstractAuthenticationToken
    }
    class JwtAuthenticationConverter {
        <<reused, SecurityConfiguration bean>>
    }
    class UserRepository {
        <<reused, user>>
        +existsByIdAndEnabledTrue(id) boolean  «new»
    }
    class StudentRepository {
        <<reused, student>>
    }
    StudentController --> StudentService
    StudentService --> StudentRepository
    StudentLifecycleAuthorizationManager --> StudentSecurityService
    StudentSecurityService --> StudentRepository
    ActiveAccountJwtAuthenticationConverter --> JwtAuthenticationConverter : delegates authorities
    ActiveAccountJwtAuthenticationConverter --> UserRepository
```

| Component | File | Reused / new |
|---|---|---|
| `StudentController.deactivateStudent` | [StudentController.java](../../backend/src/main/java/team/projectpulse/student/StudentController.java) | new method |
| `StudentService.deactivateStudent` | [StudentService.java](../../backend/src/main/java/team/projectpulse/student/StudentService.java) | new method; reuses `ObjectNotFoundException` |
| `StudentSecurityService.isCurrentUserAdminOfStudentCourse` | [StudentSecurityService.java](../../backend/src/main/java/team/projectpulse/student/StudentSecurityService.java) | new method, the mirror of `InstructorSecurityService.isCurrentUserAdminOfInstructorCourse`: compares the caller's id with `student.getSection().getCourse().getCourseAdmin()`; fails closed |
| `StudentLifecycleAuthorizationManager` | `backend/.../security/authorizationmanagers/` | new; UC-STU-reactivate-student reuses it |
| `ActiveAccountJwtAuthenticationConverter` | `backend/.../security/` | new; wraps the existing `jwtAuthenticationConverter()` bean in [SecurityConfiguration.java](../../backend/src/main/java/team/projectpulse/security/SecurityConfiguration.java) and is wired into `oauth2ResourceServer().jwt()` |
| `UserRepository.existsByIdAndEnabledTrue` | [UserRepository.java](../../backend/src/main/java/team/projectpulse/user/UserRepository.java) | new derived query |
| `StudentAlreadyDeactivatedException` | `system/exception` | new; mapped in [ExceptionHandlerAdvice.java](../../backend/src/main/java/team/projectpulse/system/exception/ExceptionHandlerAdvice.java) exactly like `SectionLockRequiredException`: HTTP 409 **and** `Result.code` `StatusCode.CONFLICT`, message "This student is already deactivated." |
| Route rule | [SecurityConfiguration.java](../../backend/src/main/java/team/projectpulse/security/SecurityConfiguration.java) | new `requestMatchers(HttpMethod.PATCH, baseUrl + "/students/{studentId}/deactivate").access(studentLifecycleAuthorizationManager)` in the `/students/**` block, above the `denyAll()` catch-all. No existing rule shadows it: the others match `/students/{studentId}` exactly |
| `deactivateStudent(studentId)` | [apis/student/index.ts](../../frontend/src/apis/student/index.ts) | new; `PATCH /students/{studentId}/deactivate` through the shared instance, typed like the module's existing calls |
| Deactivate control | [Students.vue](../../frontend/src/pages/students/Students.vue) | extended: a Deactivate button in the Operations column, shown only for an active student, opening an `ElMessageBox.confirm` (`type: 'warning'`) whose body states the consequences in the use case's Associated Information. On success, `ElMessage.success` and reload the current page. On a rejected call, add no second message (the shared instance has already shown the error toast) and reload. Only the Status tag changes from `Enabled` / `Disabled` to `Active` / `Deactivated` (the use cases' vocabulary) |
| Deactivated-evaluatee guard | [EvaluationService.java](../../backend/src/main/java/team/projectpulse/evaluation/EvaluationService.java) `addPeerEvaluation`, `updatePeerEvaluation` | extended: next to the existing same-team check, reject an evaluatee whose account is deactivated with `PeerEvaluationIllegalArgumentException` (400) |
| Section report rows | `EvaluationService.generateWeeklyPeerEvaluationReportForSection` | extended: a deactivated student gets a row only for a week in which at least one evaluation of her exists. An active student keeps a row every week, as today |
| Active-teammate list | [SubmitTeamsEvaluations.vue](../../frontend/src/pages/evaluations/SubmitTeamsEvaluations.vue) | extended: filters the `findStudentsByTeamId` result to `enabled` teammates, per the amended UC-EVA-submit-evaluation step 2. This is a convenience only; the server guard above is the enforcement |

## Sequence

### UC-STU-deactivate-student: deactivate a student

```mermaid
sequenceDiagram
  actor A as Course admin / instructor
  participant V as Students.vue
  participant API as apis/student
  participant M as StudentLifecycleAuthorizationManager
  participant S as StudentService
  participant R as StudentRepository
  A->>V: steps 1–3: find the student, choose Deactivate
  V->>A: step 4: confirm dialog stating the consequences (Associated Information)
  alt 5a: cancel
    A-->>V: Cancel (no request is sent)
  else step 5: confirm
    V->>API: deactivateStudent(id)
    API->>M: PATCH /students/{studentId}/deactivate
    M->>M: isCurrentUserAdminOfStudentCourse ∨ isCurrentUserInstructorOfStudentSection
    alt not permitted, or id unresolvable
      M-->>V: 403
    else permitted
      M->>S: deactivateStudent(studentId)
      S->>R: findById (404 if absent)
      alt 3a: already deactivated
        S-->>V: 409 CONFLICT
        V->>A: "This student is already deactivated"; reload list
      else
        S->>R: setEnabled(false), save
        S-->>V: 200 Result(StudentDto, enabled=false)
        V->>A: step 6: success message; reload list
      end
    end
  end
```

### FR-SEC-active-account: a live token of a deactivated account

```mermaid
sequenceDiagram
  participant C as SPA (deactivated user, unexpired token)
  participant F as BearerTokenAuthenticationFilter
  participant K as ActiveAccountJwtAuthenticationConverter
  participant U as UserRepository
  C->>F: any API request with Bearer JWT
  F->>K: convert(jwt)
  K->>U: existsByIdAndEnabledTrue(userId claim)
  alt account deactivated
    K-->>F: throw DisabledException
    F-->>C: 401 via CustomBearerTokenAuthenticationEntryPoint → SPA clears token, redirects to login
  else account active
    K-->>F: JwtAuthenticationToken (authorities from the claim, unchanged)
  end
```

## API contract

| Endpoint or job | Caller (who may) | Request | Success | Errors, by extension |
|---|---|---|---|---|
| `PATCH /api/v1/students/{studentId}/deactivate` | the course admin of the student's course, or an instructor assigned to the student's course section (`StudentLifecycleAuthorizationManager`); never a student | path `studentId`; no body | 200 `Result`, `data` = the `StudentDto` with `enabled = false` | 3a already deactivated → 409 `CONFLICT`; caller not permitted, or an id naming no student (the guard fails closed) → 403 |
| `POST /api/v1/evaluations`, `PUT /api/v1/evaluations/{evaluationId}` (changed) | unchanged | unchanged | unchanged | evaluatee's account is deactivated → 400, message "You cannot evaluate a deactivated teammate." (amended UC-EVA-submit-evaluation step 2) |
| `GET /api/v1/evaluations/sections/{sectionId}/week/{week}` (changed) | unchanged | unchanged | a deactivated student appears only for a week with at least one evaluation of her | — |
| Every bearer-token request (filter chain) | any authenticated user | the JWT's `userId` claim | the request continues as before | account deactivated → 401 (FR-SEC-active-account) |

## Key decisions

1. **A dedicated `PATCH …/deactivate` action, not `PUT /students/{studentId}` with `enabled = false`.** The generic update route is guarded by `UserOwnershipAuthorizationManager`, which admits the student themselves. It also mixes a lifecycle change with profile edits, and it has no place for the 409 of extension 3a. A verb sub-resource under `PATCH` follows `PATCH /teams/{teamId}/section`. UC-STU-reactivate-student gets the sibling `…/reactivate`.
2. **A new `StudentLifecycleAuthorizationManager` encodes BR-student-lifecycle exactly**: course admin of the student's course ∨ instructor assigned to the student's course section, never self. Rejected: `UserOwnershipAuthorizationManager` (admits self) and `StudentInstructorAuthorizationManager` (denies a course admin who is not also assigned to that course section). No scoped query (enforcement point 2) is needed: the only object named is the path id the manager itself resolved, and no team scope applies.
3. **FR-SEC-active-account is enforced in the JWT authentication converter, on every bearer-token request**, with one primary-key `exists` query. Rejected alternatives:
   - A token blocklist: it adds revocation state and still needs a per-request read.
   - A shorter token lifetime: the gap remains, and every user re-authenticates more often.
   - A check inside each authorization manager: it would miss the routes guarded by `hasAuthority` or `.authenticated()`, such as `POST /students/search`.

   The converter is built inside `SecurityConfiguration` (with `UserRepository` injected) and passed to `.jwt(jwt -> jwt.jwtAuthenticationConverter(…))`. It reads the `userId` claim as the `Integer` that `JwtProvider` writes; a token with no `userId` claim is treated as inactive. On an inactive account it throws `DisabledException`. `BearerTokenAuthenticationFilter` turns that into a 401 through the existing entry point, and the SPA's shared Axios instance already handles a 401 by clearing the token and redirecting to login. The same check covers a deactivated instructor (UC-INS-deactivate-instructor) at no extra cost.
4. **Extension 3a returns 409, not an idempotent 200**, so an actor working from a stale list is told the student was already deactivated, as the extension requires.
5. **A deactivated student stays on her team.** Deactivation changes only the `enabled` flag. Her team membership, her weekly activity reports, and the peer evaluations she wrote and received all stay as they are. The spec already assumes this: BR-submission-owed's "every member of her current team whose account is active" only filters anyone if a deactivated student can still be a team member. The two options have different consequences:
   - **Staying on the team (chosen).** Reactivation puts her straight back on her team. Her history stays in the team's and the course section's reports. The cost is that every consumer of a team roster must tell active and deactivated members apart. This design handles the three current consumers: the owed computation (`SubmissionStatusService`, already filtering), evaluation submission (decision 6), and the section peer-evaluation report (decision 7).
   - **Removal (rejected).** It would make the roster filter unnecessary, and the existing same-team check would reject teammates' evaluations of her for free. But the section peer-evaluation report is built from current team membership, so she would vanish from it for past weeks too. Nothing would record her former team, so UC-STU-reactivate-student could not restore it, and an instructor would have to re-assign her through UC-TEA-assign-students. It would also fold UC-TEA-remove-student into deactivation.
6. **Teammates stop evaluating a deactivated student, enforced on the server.** BR-submission-owed already counts only active teammates, but UC-EVA-submit-evaluation step 2 said "every team member", and nothing on the server stopped an evaluation of a deactivated teammate. Step 2 now reads "every team member whose account is active". `addPeerEvaluation` and `updatePeerEvaluation` reject a deactivated evaluatee next to the existing same-team check, and the form filters the roster it already fetches. Rejected:
   - Filtering only in the form: a client-side check is not access control.
   - Making `GET /students/teams/{teamId}` return active students only: it is a general team-roster read, and an instructor's view of the team should still show the deactivated member.

   An evaluation of her written before deactivation is kept and counts. An edit to it after deactivation is refused.
7. **The section peer-evaluation report shows a deactivated student only for weeks in which she was evaluated.** It lists every student on a team. Because she stays on her team (decision 5), she would otherwise get a row with an average of 0.0 for every week after deactivation, which reads as her teammates rating her zero. Rejected:
   - Dropping every deactivated student from the report: it erases the weeks she was actually evaluated.
   - Marking the row "deactivated" with no score: it changes the report's shape for one case.

   An active student no one evaluated still gets her 0.0 row, as today. That is existing behaviour, not this use case's concern.

## Data model

No delta. The `enabled` column of `PeerEvaluationUser` already exists; the only addition is a derived query on `UserRepository`.

## Reuse & cross-cutting

- **Reused:** the `Result` envelope and `ExceptionHandlerAdvice`; the `*SecurityService` + `AuthorizationManager` pair with fail-closed guards (architecture-of-record, Authorization); the existing JWT authorities converter; the shared Axios instance's 401/403 handling.
- **Not used:** email. This use case notifies no one but the actor. UC-STU-reactivate-student notifies the student.

## Tests

| Use case, flow | Level | Asserts |
|---|---|---|
| UC-STU-deactivate-student, main success (course admin) | integration (`StudentIntegrationTest`) | the course admin `PATCH`es → 200, `data.enabled` is false; a subsequent login as that student → 401 |
| UC-STU-deactivate-student, main success (instructor of the course section) | integration | an instructor assigned to the student's course section → 200 |
| UC-STU-deactivate-student, main success | unit (`StudentServiceTest`) | `enabled` is set to false and saved; the student's team is unchanged |
| UC-STU-deactivate-student, 3a already deactivated | unit + integration | 409 `CONFLICT`; no save |
| UC-STU-deactivate-student, 5a cancel | — | client-only, and no request is sent; no backend test |
| UC-STU-deactivate-student, caller is a student (self or another) | integration | 403; `enabled` unchanged |
| UC-STU-deactivate-student, instructor of another course section | integration | 403 |
| UC-STU-deactivate-student, unknown student id | integration | 403 (guard fails closed) |
| FR-SEC-active-account, live token | integration | a token obtained before deactivation, used on a protected route afterwards (e.g. `GET /students/{own id}`) → 401 |
| FR-SEC-active-account, active account | unit (`ActiveAccountJwtAuthenticationConverterTest`) | an active user converts with the claim's authorities unchanged; a deactivated one, or a token with no `userId` claim, throws `DisabledException` |
| UC-STU-deactivate-student, team membership kept | integration | after deactivation, `GET /students/teams/{teamId}` still lists the student, with `enabled` false |
| UC-EVA-submit-evaluation step 2 (amended), evaluating a deactivated teammate | unit (`EvaluationServiceTest`) + integration | `POST /evaluations` and `PUT /evaluations/{evaluationId}` naming a deactivated evaluatee → 400; nothing saved |
| UC-EVA-submit-evaluation step 2 (amended), evaluation written before deactivation | unit | it is still returned and still counts in the evaluatee's average |
| Section peer-evaluation report, deactivated student | unit (`EvaluationServiceTest`) | a deactivated student has a row for a week with evaluations of her and no row for a week without; an active student with no evaluations still has a 0.0 row |
| UC-EVA-submit-evaluation step 2 (amended), form | manual / Cypress (no frontend unit tests exist) | after a teammate is deactivated, the peer evaluation form lists only the active teammates, self included |

Fixtures: the integration tests use the `DataInitializer` seed (course admin `b.wei@abc.edu`, an instructor assigned to the student's course section, an instructor of another course section, students). Each test that deactivates a seed student must leave the seed as it found it, following the existing isolation pattern of `StudentIntegrationTest`. Extension 3a deactivates first, then repeats the request. Tokens come from the existing login helper of the integration tests.

## Open questions / risks

- **The generic `enabled` toggle stays for now.** `StudentService.updateStudent` and `EditStudentForm.vue` still write `enabled`, so until UC-STU-reactivate-student is built it remains the only way to reactivate a student. Remove both when the `…/reactivate` endpoint lands; until then there are two ways to deactivate.
- **Future team-roster consumers.** Keeping a deactivated student on her team (decision 5) makes "active member" a condition every new team-roster consumer must apply. Three consumers are handled today. A new team-level report or count that iterates `Team.getStudents()` unfiltered will count a withdrawn student.
- **Cost of the per-request check.** It adds one indexed primary-key query to every authenticated request. That is negligible at single-tenant scale. If it ever shows up in tracing, cache it with a short TTL rather than dropping it.
- **Out of scope, logged as OI-65:** `DELETE /students/{studentId}` is guarded by `UserOwnershipAuthorizationManager`, so a student passes the route rule for their own account, contrary to BR-student-lifecycle (course admin only).
