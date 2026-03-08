# KidBank — Virtual Savings Tracker

A local-only Spring Boot application for managing virtual savings accounts for children. Parents deposit/withdraw funds, set interest rates, and view ledgers. Children get a read-only view of their own account.

---

## Prerequisites

- **Java 17+** (Java 21 also supported)
- **Maven 3.8+**

Verify with:
```bash
java -version
mvn -version
```

---

## Build

```bash
mvn clean package -DskipTests
```

The JAR is created at `target/savings-tracker.jar`.

---

## Run

```bash
java -jar target/savings-tracker.jar
```

Or, during development:

```bash
mvn spring-boot:run
```

The app starts on **http://localhost:8080**.

---

## Configuring the Bootstrap Parent User

Edit `src/main/resources/application.properties` before first launch:

```properties
app.bootstrap.enabled=true
app.bootstrap.parent.username=admin
app.bootstrap.parent.password=changeme123
app.bootstrap.parent.displayName=Parent Admin
```

The parent account is created **once** on first startup. After that, `bootstrap_completed=true` is stored in the database and the bootstrap step is skipped permanently — even if `app.bootstrap.enabled` remains `true`.

**Change the default password immediately after first login.**

---

## Step-by-Step: Getting Started

1. Start the app: `java -jar target/savings-tracker.jar`
2. Open **http://localhost:8080/login**
3. Log in with your configured parent credentials (default: `admin` / `changeme123`)
4. On the **Parent Dashboard**, fill in the "Add a Child Account" form with:
   - A username (3–50 chars, alphanumeric/underscore/hyphen)
   - An optional display name
   - A password (8+ chars)
   - An initial annual interest rate (0–1, e.g. `0.05` for 5%)
5. Click **Add** — the child user and their savings account are created
6. Use **Deposit** / **Withdraw** / **Set Rate** / **View Ledger** per account

The child can log in and view their own balance and transaction history (read-only).

---

## Interest Calculation

Interest is **compounded monthly** and applied automatically on the **1st of each month at midnight** (server local time).

**Formula:**
```
monthly_rate = annual_rate / 12
interest     = balance × monthly_rate
interest     = round(interest, 2, HALF_UP)
```

**Example:** $1,000.00 balance at 5% annual → `0.05 / 12 = 0.004167` → interest = **$4.17**

The system guarantees interest is applied **at most once per calendar month** per account using two idempotency layers:
1. Application check: `lastInterestAppliedMonth` on the account
2. Database check: existing INTEREST transaction for same account + month

Missed months (e.g. if the server was offline on the 1st) are **not back-filled**.

---

## Safely Stopping the App

Apache Derby holds an exclusive lock on the `./data/` directory. The app registers a shutdown hook that issues the Derby shutdown command cleanly.

**Always stop the app gracefully** (do not `kill -9`):

```bash
# If running as a foreground process:
Ctrl+C

# If running as a background process, find the PID and send SIGTERM:
kill <PID>
```

After a clean stop you will see in the logs:
```
INFO  ... Apache Derby shut down cleanly.
```

If the app fails to start with a lock error, it means a previous instance was not shut down cleanly. Wait a few seconds and retry.

---

## Database

Derby database files are stored in `./data/savingsdb/` relative to the working directory. This directory is **excluded from git** via `.gitignore`. Never commit it.

To reset the database (development only): stop the app, delete the `./data/` directory, and restart.
