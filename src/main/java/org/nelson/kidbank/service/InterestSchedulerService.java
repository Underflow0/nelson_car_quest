package org.nelson.kidbank.service;

import org.nelson.kidbank.entity.*;
import org.nelson.kidbank.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InterestSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(InterestSchedulerService.class);
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final long SYSTEM_USER_ID = 0L;
    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final SavingsAccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public InterestSchedulerService(SavingsAccountRepository accountRepository,
                                    TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /** Runs at midnight on the 1st of each month. */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void applyMonthlyInterest() {
        String yearMonth = YearMonth.now().format(YEAR_MONTH_FMT);
        log.info("Interest scheduler running for month: {}", yearMonth);

        List<SavingsAccount> activeAccounts = accountRepository.findByStatus(SavingsAccount.Status.ACTIVE);

        for (SavingsAccount account : activeAccounts) {
            try {
                applyInterestToAccount(account.getId(), yearMonth);
            } catch (Exception e) {
                log.error("Failed to apply interest to account {}: {}", account.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Apply interest to a single account in its own transaction boundary.
     * Each account is independent — one failure must not roll back others.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyInterestToAccount(Long accountId, String yearMonth) {
        SavingsAccount account = accountRepository.findById(accountId).orElse(null);
        if (account == null) return;

        // Layer 1 — Application-level idempotency check
        if (yearMonth.equals(account.getLastInterestAppliedMonth())) {
            log.warn("Interest already applied for account {} for month {} — skipping.", accountId, yearMonth);
            return;
        }

        // Layer 2 — Database-level idempotency check (catches duplicate runs)
        if (transactionRepository.findByAccountIdAndTypeAndYearMonth(
                accountId, Transaction.Type.INTEREST, yearMonth).isPresent()) {
            log.warn("Interest transaction already exists for account {} month {} — skipping.", accountId, yearMonth);
            account.setLastInterestAppliedMonth(yearMonth);
            accountRepository.save(account);
            return;
        }

        if (account.getStatus() == SavingsAccount.Status.CLOSED) {
            log.info("Skipping closed account {}.", accountId);
            return;
        }

        BigDecimal balance = account.getBalance();
        BigDecimal annualRate = account.getInterestRateAnnual();

        // Monthly rate = annualRate / 12
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal newBalance = balance.add(interest).setScale(2, RoundingMode.HALF_UP);
        account.setBalance(newBalance);
        account.setLastInterestAppliedMonth(yearMonth);
        accountRepository.save(account);

        Transaction tx = new Transaction();
        tx.setAccountId(accountId);
        tx.setType(Transaction.Type.INTEREST);
        tx.setAmount(interest);
        tx.setBalanceAfter(newBalance);
        tx.setNote("Monthly interest for " + yearMonth);
        tx.setCreatedByUserId(SYSTEM_USER_ID);
        tx.setYearMonth(yearMonth);
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        AUDIT.info("action=INTEREST_APPLIED accountId={} yearMonth={} amount={} newBalance={} outcome=SUCCESS",
                accountId, yearMonth, interest, newBalance);
        log.info("Interest applied: account={} month={} amount={} newBalance={}",
                accountId, yearMonth, interest, newBalance);
    }
}
