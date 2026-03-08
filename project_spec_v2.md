**Local-Only Spring Boot Virtual Savings Tracker**

Parents + Kids

*Project Specification & Code Generation Prompt*

Version 2.0 --- Enhanced

**1. Technology Stack (Required Versions)**

All technology choices below are prescriptive. Code generation agents
must not substitute alternatives unless explicitly noted.

-   Java 17 LTS (minimum; Java 21 LTS also acceptable)

-   Spring Boot 3.x (use 3.2.x or later --- do not use deprecated Spring
    Boot 2.x patterns)

-   Spring Security 6.x (bundled with Spring Boot 3.x)

-   Spring Data JPA + Hibernate 6.x

-   Apache Derby embedded database (embedded mode only)

-   Thymeleaf 3.x for server-rendered templates

-   Bootstrap 5.x via CDN for UI styling

-   Maven or Gradle build system (Maven preferred for simplicity)

> **Note:** *The ./data/ directory containing the Derby database must be
> added to .gitignore. Never commit database files.*

**2. Objective**

Build a local-only Java Spring Boot web application to manage a virtual
savings account system for children. The app will run on a household
server and must persist data to disk so it survives reboots. It
supports:

-   A ledger with full transaction history

-   Monthly compounded interest applied automatically on the 1st of each
    month

-   Parent accounts with full administrative control over child accounts
    and transactions

-   Child accounts that are view-only

**3. Roles & Permissions**

Implement role-based access with exactly two roles. All permission
enforcement must be server-side --- UI restrictions alone are
insufficient.

**3.1 PARENT Role**

-   Create child users (username + password + optional display name)

-   Create exactly one savings account per child

-   Deposit funds into child accounts

-   Withdraw funds from child accounts

-   Set and update the annual interest rate per child account

-   View ledgers for all child accounts they manage

-   Close/disable a child account (recommended)

-   Cannot access or manage accounts belonging to other parents

**3.2 CHILD Role**

-   View current balance (their own account only)

-   View ledger / transaction history (their own account only)

-   No ability to deposit, withdraw, or modify any settings

> **Enforcement:** *All authorization checks must be enforced at the
> service layer, not only in controllers or the UI. A child user posting
> directly to a parent endpoint must receive a 403 Forbidden.*

**4. Authentication & Security**

**4.1 Login**

-   Form-based login page at /login

-   Session-based authentication (Spring Security default)

-   Store all passwords using BCrypt hashing --- never store plaintext

-   Users are stored in the database, not in-memory

**4.2 Security Hardening (Required)**

-   CSRF protection must be explicitly enabled (Spring Security default
    for form-based apps --- confirm it is not disabled)

-   Session timeout: 30 minutes of inactivity

-   Failed login lockout: after 5 consecutive failed attempts, lock the
    account for 15 minutes to prevent brute-force attacks on child
    accounts

-   All sensitive endpoints must require authentication ---
    unauthenticated requests redirect to /login

-   Passwords must never appear in application logs, even during
    bootstrap

**5. Persistence (Disk-Based)**

Use Apache Derby in embedded mode. The database files must be stored in
a ./data/ subdirectory relative to the application working directory.

**5.1 JDBC Configuration**

> spring.datasource.url=jdbc:derby:./data/savingsdb;create=true
>
> spring.datasource.driver-class-name=org.apache.derby.jdbc.EmbeddedDriver

**5.2 Derby Shutdown Hook (Critical)**

Apache Derby in embedded mode acquires an exclusive lock on the database
directory. Only one JVM may hold this lock at a time. The application
must register a JVM shutdown hook that issues the Derby shutdown command
on application stop, or subsequent restarts will fail with a lock error.

-   Register a \@PreDestroy method or
    ApplicationListener\<ContextClosedEvent\> that calls:

> DriverManager.getConnection(\"jdbc:derby:;shutdown=true\")

-   Catch the expected SQLNonTransientConnectionException (Derby always
    throws on shutdown --- this is normal)

-   Log the clean shutdown at INFO level

**5.3 Schema Migration**

Use Flyway or Liquibase for schema management. This ensures the schema
is created/migrated automatically on startup and makes the database
structure self-documenting. Flyway is recommended for its simplicity.

**5.4 ORM**

Use Spring Data JPA (Hibernate) for all persistence. Use BigDecimal for
all monetary values --- never use float or double.

**6. Data Model**

**6.1 User Entity**

-   id (Long, auto-generated primary key)

-   username (String, unique, not null)

-   displayName (String, optional, max 100 characters)

-   passwordHash (String, BCrypt)

-   role (Enum: PARENT, CHILD)

-   parentId (Long, nullable --- populated for CHILD users, references
    parent\'s id)

-   enabled (boolean, default true)

-   failedLoginAttempts (int, default 0)

-   lockoutUntil (LocalDateTime, nullable)

-   createdAt (LocalDateTime)

**6.2 SavingsAccount Entity**

-   id (Long, auto-generated primary key)

-   childUserId (Long, unique FK to User --- unique constraint enforces
    one account per child)

-   parentUserId (Long, FK to User)

-   balance (BigDecimal, scale=2, default 0.00)

-   interestRateAnnual (BigDecimal, scale=4, e.g. 0.0500 for 5%)

-   lastInterestAppliedMonth (String, format YYYY-MM, nullable)

-   status (Enum: ACTIVE, CLOSED)

-   version (Long, for optimistic locking via \@Version)

-   createdAt (LocalDateTime)

-   updatedAt (LocalDateTime)

> **Constraint:** *Add a unique database constraint on childUserId. The
> service layer must also check and throw a descriptive exception if a
> parent attempts to create a second account for the same child.*

**6.3 Transaction Entity (Ledger)**

-   id (Long, auto-generated primary key)

-   accountId (Long, FK to SavingsAccount)

-   timestamp (LocalDateTime, set at creation, never mutable)

-   type (Enum: DEPOSIT, WITHDRAWAL, INTEREST)

-   amount (BigDecimal, scale=2, always positive --- direction implied
    by type)

-   balanceAfter (BigDecimal, scale=2, snapshot of balance after this
    transaction)

-   note (String, optional, max 500 characters)

-   createdByUserId (Long, FK to User --- parent\'s id for
    DEPOSIT/WITHDRAWAL; use a designated system user id, e.g. 0 or a
    SYSTEM user, for INTEREST)

-   yearMonth (String, format YYYY-MM --- populated only for INTEREST
    type, used for idempotency)

> **Immutability:** *Transactions are append-only. No UPDATE or DELETE
> operations on the Transaction table are permitted through any service
> or controller.*

**6.4 AppSetting Entity**

A simple key-value table used for persistent application state flags.

-   key (String, primary key)

-   value (String)

Used to store bootstrap_completed=true after the initial parent user has
been created. This decouples bootstrap state from user count.

**7. Input Validation**

Apply Bean Validation (@Valid, \@NotNull, \@Size, \@DecimalMin, etc.) to
all form-backed objects. Validation must be enforced at both the
controller and service layers.

-   Username: 3--50 characters, alphanumeric plus underscore and hyphen
    only, unique

-   Display name: 1--100 characters (if provided)

-   Password: 8--100 characters minimum

-   Deposit/Withdrawal amount: must be positive (\> 0), max 2 decimal
    places

-   Interest rate: must be \>= 0 and \<= 1.0 (i.e. 0% to 100% annual),
    max 4 decimal places

-   Notes/comments: max 500 characters

**7.1 Error Handling**

All validation errors and business rule violations must result in
user-friendly error messages rendered back to the form. They must not
leak stack traces or internal details.

-   Overdraft attempt: return HTTP 400 with message \'Insufficient funds
    --- withdrawal would result in a negative balance.\' No transaction
    is created.

-   Duplicate username: return HTTP 400 with message \'Username already
    exists.\'

-   Invalid interest rate: return HTTP 400 with message \'Interest rate
    must be between 0% and 100%.\'

-   Attempt to transact on a CLOSED account: return HTTP 400 with
    message \'This account is closed and cannot accept transactions.\'

-   Cross-parent access attempt: return HTTP 403 Forbidden.

-   Child attempting write operation: return HTTP 403 Forbidden.

-   All unhandled server errors: return a generic HTTP 500 page ---
    never expose stack traces to the browser.

**8. Business Rules**

**8.1 One Account Per Child**

-   Enforced at the database level via unique constraint on
    SavingsAccount.childUserId

-   Enforced at the service level --- throw
    AccountAlreadyExistsException if attempted

-   Surfaced to the parent UI with a clear error message

**8.2 No Overdrafts**

-   Withdrawal amount must not cause balance to go below 0.00

-   Check performed inside a \@Transactional service method before any
    update

-   No transaction record is created on a rejected withdrawal

**8.3 Transactional Integrity**

-   Every balance change (deposit, withdrawal, interest) must update the
    account balance AND create a Transaction record in a single atomic
    \@Transactional operation

-   If either operation fails, the entire transaction must be rolled
    back

-   Use optimistic locking (@Version on SavingsAccount) to prevent lost
    updates from concurrent access

**8.4 Account Status**

-   CLOSED accounts must reject all deposits, withdrawals, and interest
    application

-   Child users with CLOSED accounts can still view their historical
    ledger

-   Closing an account does not delete any data

**9. Monthly Interest (Compounded Monthly)**

**9.1 Calculation**

Interest is compounded monthly. Apply using the following formula:

-   Annual rate: rAnnual (stored as decimal, e.g. 0.05 for 5%)

-   Monthly rate: rMonthly = rAnnual / 12

-   Interest amount: interest = balance × rMonthly

-   Round to nearest cent: setScale(2, RoundingMode.HALF_UP)

-   Fixture example: \$1,000.00 balance at 5% annual → rMonthly =
    0.004167 → interest = \$4.17

**9.2 Scheduling**

Use Spring\'s \@Scheduled annotation to run on the 1st day of each month
at midnight (local server timezone).

> \@Scheduled(cron = \"0 0 0 1 \* ?\")

-   The scheduler iterates all ACTIVE accounts and applies interest to
    each

-   Each account is processed in its own \@Transactional boundary so a
    failure in one account does not prevent others from being processed

-   Log each interest application at INFO level: account id, amount
    applied, new balance

**9.3 Idempotency (Critical)**

The scheduler must never double-apply interest for the same month,
regardless of restarts or accidental duplicate runs. Implement both
layers of protection:

-   Layer 1 --- Application check: before applying interest, read
    lastInterestAppliedMonth on the account. If it equals the current
    YearMonth (formatted as YYYY-MM), skip this account and log a
    warning.

-   Layer 2 --- Database constraint: add a unique constraint on
    (accountId, yearMonth) in the Transaction table for rows where type
    = INTEREST. If a duplicate insert is attempted, catch the constraint
    violation and log it --- do not propagate as an unhandled error.

-   Back-fill policy: if the app was offline on the 1st of a month, do
    NOT back-fill interest for missed months. Apply interest for the
    current month only on the next run.

**10. Bootstrapping the Initial Parent User**

**10.1 Configuration Keys**

The following keys must be documented in application.properties:

> app.bootstrap.enabled=true
>
> app.bootstrap.parent.username=admin
>
> app.bootstrap.parent.password=changeme123
>
> app.bootstrap.parent.displayName=Parent Admin

**10.2 Bootstrap Behavior**

-   On application startup, check the AppSetting table for the key
    bootstrap_completed

-   If bootstrap_completed exists and equals true, skip bootstrap
    entirely regardless of app.bootstrap.enabled

-   If bootstrap_completed does not exist AND
    app.bootstrap.enabled=true, create the parent user with the
    configured credentials (BCrypt the password) and insert
    bootstrap_completed=true into AppSetting

-   Never log the plaintext password --- not even at DEBUG level

-   Never recreate the bootstrap user if it already exists (check by
    username before inserting)

> **Why AppSetting:** *Using a persistent DB flag instead of checking
> user count prevents accidental re-bootstrap if all users are deleted
> for other reasons.*

**11. Audit Logging**

All parent-initiated actions must produce a structured audit log entry.
This is critical for a financial application and aids debugging.

-   Log to a dedicated audit_log table OR to structured application logs
    (SLF4J + Logback), clearly labeled as AUDIT

-   Minimum fields: timestamp, actorUserId, actorUsername, action,
    targetAccountId (if applicable), amount (if applicable), outcome
    (SUCCESS/FAILURE)

-   Actions to audit: CREATE_CHILD, DEPOSIT, WITHDRAWAL,
    SET_INTEREST_RATE, CLOSE_ACCOUNT, LOGIN_SUCCESS, LOGIN_FAILURE,
    INTEREST_APPLIED

-   Audit entries are append-only and must never be deleted or modified

**12. UI Requirements**

**12.1 Parent Dashboard**

-   List all child accounts with name, current balance, interest rate,
    and account status

-   Create child user form (username, display name, password)

-   Per-account: deposit form, withdrawal form, update interest rate
    form

-   Link to view child\'s full ledger

-   Close account button with confirmation prompt

**12.2 Child Dashboard**

-   Display child\'s name and current balance prominently

-   Display ledger table (newest first)

-   No controls for transactions or settings of any kind

**12.3 Ledger View**

-   Columns: Date/Time, Type, Amount, Note, Balance After

-   Sorted newest-first

-   Type displayed as human-readable label (Deposit, Withdrawal,
    Interest)

-   Optional: filter by type and/or date range (nice-to-have)

**12.4 UI Approach**

-   Server-rendered Thymeleaf templates (no single-page app frameworks)

-   Bootstrap 5 via CDN for layout and styling

-   Flash messages for success and error feedback on all form
    submissions

-   All error messages from validation and business rules must display
    inline on the relevant form

**13. Endpoint Structure**

The following routes must exist. Exact sub-paths may vary but
functionality is required.

-   GET /login --- Login form

-   POST /login --- Login form submission (handled by Spring Security)

-   POST /logout --- Logout (handled by Spring Security)

-   GET /parent/dashboard --- Parent home, list of children and accounts

-   POST /parent/children --- Create new child user + account

-   POST /parent/accounts/{accountId}/deposit --- Deposit funds

-   POST /parent/accounts/{accountId}/withdraw --- Withdraw funds

-   POST /parent/accounts/{accountId}/interest-rate --- Update interest
    rate

-   POST /parent/accounts/{accountId}/close --- Close account

-   GET /parent/accounts/{accountId}/ledger --- View child ledger

-   GET /child/dashboard --- Child home

-   GET /child/accounts/{accountId}/ledger --- Child ledger view

**13.1 Authorization Rules**

-   Parent endpoints: require PARENT role AND parentUserId ==
    currentUserId on the target account

-   Child endpoints: require CHILD role AND childUserId == currentUserId
    on the target account

-   Any violation returns HTTP 403 Forbidden

**14. Testing Requirements**

Provide unit and integration tests using JUnit 5 and Spring Boot Test.
Mock external dependencies where appropriate. Tests must be runnable
with mvn test or gradle test.

**14.1 Required Test Cases**

**Business Logic**

-   Deposit increases balance by the correct amount and creates a
    DEPOSIT ledger entry

-   Withdrawal decreases balance by the correct amount and creates a
    WITHDRAWAL ledger entry

-   Withdrawal with amount \> balance throws InsufficientFundsException
    and creates no ledger entry

-   Interest calculation: \$1,000.00 at 5% annual produces \$4.17 for
    month 1 (fixture test)

**Idempotency**

-   Interest applied once for a given month; second run in same month is
    skipped

-   Bootstrap creates parent user exactly once; second startup does not
    create duplicate

**Authorization / Role Enforcement**

-   Child user POSTing to deposit endpoint returns 403

-   Child user POSTing to withdraw endpoint returns 403

-   Child user POSTing to interest-rate endpoint returns 403

-   Parent A cannot access Parent B\'s child accounts (cross-parent
    isolation)

**Account Rules**

-   Creating a second account for the same child throws
    AccountAlreadyExistsException

-   Deposit to a CLOSED account returns 400 with appropriate error
    message

-   Withdrawal from a CLOSED account returns 400 with appropriate error
    message

-   Interest scheduler skips CLOSED accounts

**Security**

-   Account lockout activates after 5 failed login attempts

-   Unauthenticated request to /parent/dashboard redirects to /login

**15. Deliverables**

-   Working Spring Boot application runnable with java -jar or mvn
    spring-boot:run

-   Disk-persistent Derby database stored in ./data/ directory

-   ./data/ added to .gitignore

-   All tests passing with mvn test

**15.1 README Requirements**

The README must document all of the following:

-   Prerequisites: Java version, Maven/Gradle version

-   Build command (e.g. mvn clean package)

-   Run command (e.g. java -jar target/savings-tracker.jar)

-   Default port and URL to access the app (e.g. http://localhost:8080)

-   How to configure the bootstrap parent user in application.properties
    (with example values)

-   Step-by-step: how to log in and create child accounts

-   How interest is computed (formula) and when it is scheduled

-   How to safely stop the app (to avoid Derby lock issues)

**16. Implementation Notes (Do Not Skip)**

-   Use BigDecimal everywhere for money --- never float or double

-   All monetary BigDecimal values must use scale=2 and
    RoundingMode.HALF_UP

-   Ensure every balance-changing operation (deposit, withdraw,
    interest) is performed within a single \@Transactional method that
    updates both the account balance and creates the ledger entry
    atomically

-   Use \@Version on SavingsAccount for optimistic locking to prevent
    lost updates

-   Implement the Derby shutdown hook to avoid lock errors on restart
    (see Section 5.2)

-   Never expose stack traces to the browser --- use a global
    \@ControllerAdvice for exception handling

-   The scheduler must use \@Scheduled with a cron expression, not a
    fixed delay, so it triggers on a calendar boundary

-   All timestamps must use LocalDateTime.now() --- do not rely on
    database auto-timestamps for ledger records, as the application
    timestamp is the authoritative record
