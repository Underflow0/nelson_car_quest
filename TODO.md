# Implementation Plan: Virtual Savings Tracker

## Status Legend
- [ ] Not started
- [x] Complete
- [-] In progress

---

## Phase 1: Project Scaffold
- [x] Create `pom.xml` with all required dependencies
- [x] Create Maven directory structure (`org.nelson.kidbank`)
- [x] Create `src/main/resources/application.properties`
- [x] Create `.gitignore` (include `./data/`)

## Phase 2: Database Schema (Flyway)
- [x] `V1__init.sql` — users, savings_accounts, transactions, app_settings tables
- [x] `V2__interest_idempotency_constraint.sql` — indexes for performance and idempotency

## Phase 3: Domain Entities
- [x] `User.java`
- [x] `SavingsAccount.java`
- [x] `Transaction.java`
- [x] `AppSetting.java`

## Phase 4: Repositories
- [x] `UserRepository.java`
- [x] `SavingsAccountRepository.java`
- [x] `TransactionRepository.java`
- [x] `AppSettingRepository.java`

## Phase 5: Exceptions & DTOs
- [x] `InsufficientFundsException.java`
- [x] `AccountAlreadyExistsException.java`
- [x] `AccountClosedException.java`
- [x] `DuplicateUsernameException.java`
- [x] `TransactionForm.java`
- [x] `CreateChildForm.java`
- [x] `InterestRateForm.java`

## Phase 6: Security Configuration
- [x] `SecurityConfig.java` — form login, CSRF enabled, session timeout 30m, role-based URL security
- [x] `CustomUserDetailsService.java` — load user from DB, check lockout
- [x] `LoginSuccessHandler.java` — reset failed attempts, redirect by role
- [x] `LoginFailureHandler.java` — increment failed attempts, lock after 5
- [x] `DerbyShutdownConfig.java` — `@PreDestroy` shutdown hook

## Phase 7: Bootstrap
- [x] `BootstrapRunner.java` — `ApplicationRunner` that checks AppSetting and creates initial parent user

## Phase 8: Services
- [x] `UserService.java` — create child user, find users
- [x] `AccountService.java` — create account, deposit, withdraw, set interest rate, close account
- [x] `InterestSchedulerService.java` — `@Scheduled` cron, idempotency check, apply interest

## Phase 9: Controllers & Global Exception Handler
- [x] `LoginController.java` — GET /login redirect
- [x] `ParentController.java` — dashboard, create child, deposit, withdraw, interest rate, close, ledger
- [x] `ChildController.java` — dashboard, ledger
- [x] `GlobalExceptionHandler.java` — `@ControllerAdvice`, no stack traces to browser

## Phase 10: Thymeleaf Templates
- [x] `fragments/layout.html` — base layout with Bootstrap 5 CDN
- [x] `login.html`
- [x] `parent/dashboard.html`
- [x] `parent/ledger.html`
- [x] `child/dashboard.html`
- [x] `child/ledger.html`
- [x] `error/generic.html`

## Phase 11: Tests (17/17 passing)
- [x] `AccountServiceTest.java` — deposit, withdraw, overdraft, interest calc, closed account
- [x] `InterestSchedulerTest.java` — idempotency (same month skipped), closed account skipped
- [x] `BootstrapRunnerTest.java` — creates once, not twice
- [x] `ParentControllerSecurityTest.java` — CHILD gets 403 on write endpoints, unauthenticated redirects

## Phase 12: README
- [x] Prerequisites, build, run, port, bootstrap config, usage walkthrough, interest formula, safe shutdown

---

## Build & Run

```bash
mvn clean package -DskipTests
java -jar target/savings-tracker.jar
# Open http://localhost:8080
```

## All tests passing
```
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```
