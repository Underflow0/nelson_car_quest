package org.nelson.kidbank.service;

import org.nelson.kidbank.entity.*;
import org.nelson.kidbank.exception.*;
import org.nelson.kidbank.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final SavingsAccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public AccountService(SavingsAccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SavingsAccount createAccount(Long childUserId, Long parentUserId, BigDecimal annualRate) {
        if (accountRepository.existsByChildUserId(childUserId)) {
            throw new AccountAlreadyExistsException(childUserId);
        }

        SavingsAccount account = new SavingsAccount();
        account.setChildUserId(childUserId);
        account.setParentUserId(parentUserId);
        account.setBalance(BigDecimal.ZERO.setScale(2));
        account.setInterestRateAnnual(annualRate.setScale(4, RoundingMode.HALF_UP));
        account.setStatus(SavingsAccount.Status.ACTIVE);

        SavingsAccount saved = accountRepository.save(account);
        AUDIT.info("action=CREATE_ACCOUNT actorUserId={} childUserId={} accountId={} outcome=SUCCESS",
                parentUserId, childUserId, saved.getId());
        return saved;
    }

    @Transactional
    public Transaction deposit(Long accountId, BigDecimal amount, String note, Long actorParentId) {
        SavingsAccount account = getAccountForParent(accountId, actorParentId);
        assertAccountActive(account);

        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalance = account.getBalance().add(scaled).setScale(2, RoundingMode.HALF_UP);
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction tx = buildTransaction(account.getId(), Transaction.Type.DEPOSIT, scaled, newBalance, note, actorParentId, null);
        Transaction saved = transactionRepository.save(tx);

        AUDIT.info("action=DEPOSIT actorUserId={} accountId={} amount={} newBalance={} outcome=SUCCESS",
                actorParentId, accountId, scaled, newBalance);
        return saved;
    }

    @Transactional
    public Transaction withdraw(Long accountId, BigDecimal amount, String note, Long actorParentId) {
        SavingsAccount account = getAccountForParent(accountId, actorParentId);
        assertAccountActive(account);

        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalance = account.getBalance().subtract(scaled).setScale(2, RoundingMode.HALF_UP);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            AUDIT.warn("action=WITHDRAWAL actorUserId={} accountId={} amount={} outcome=FAILURE reason=INSUFFICIENT_FUNDS",
                    actorParentId, accountId, scaled);
            throw new InsufficientFundsException();
        }

        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction tx = buildTransaction(account.getId(), Transaction.Type.WITHDRAWAL, scaled, newBalance, note, actorParentId, null);
        Transaction saved = transactionRepository.save(tx);

        AUDIT.info("action=WITHDRAWAL actorUserId={} accountId={} amount={} newBalance={} outcome=SUCCESS",
                actorParentId, accountId, scaled, newBalance);
        return saved;
    }

    @Transactional
    public void setInterestRate(Long accountId, BigDecimal rate, Long actorParentId) {
        SavingsAccount account = getAccountForParent(accountId, actorParentId);
        account.setInterestRateAnnual(rate.setScale(4, RoundingMode.HALF_UP));
        accountRepository.save(account);

        AUDIT.info("action=SET_INTEREST_RATE actorUserId={} accountId={} rate={} outcome=SUCCESS",
                actorParentId, accountId, rate);
    }

    @Transactional
    public void closeAccount(Long accountId, Long actorParentId) {
        SavingsAccount account = getAccountForParent(accountId, actorParentId);
        account.setStatus(SavingsAccount.Status.CLOSED);
        accountRepository.save(account);

        AUDIT.info("action=CLOSE_ACCOUNT actorUserId={} accountId={} outcome=SUCCESS",
                actorParentId, accountId);
    }

    public List<Transaction> getLedger(Long accountId, Long actorParentId) {
        SavingsAccount account = getAccountForParent(accountId, actorParentId);
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId());
    }

    public List<Transaction> getLedgerForChild(Long accountId, Long childUserId) {
        SavingsAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
        if (!account.getChildUserId().equals(childUserId)) {
            throw new AccessDeniedException("Access denied.");
        }
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    public Optional<SavingsAccount> findByChildUserId(Long childUserId) {
        return accountRepository.findByChildUserId(childUserId);
    }

    public List<SavingsAccount> findByParentUserId(Long parentUserId) {
        return accountRepository.findByParentUserId(parentUserId);
    }

    public Optional<SavingsAccount> findById(Long accountId) {
        return accountRepository.findById(accountId);
    }

    // --- Internal helpers ---

    private SavingsAccount getAccountForParent(Long accountId, Long parentId) {
        SavingsAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
        if (!account.getParentUserId().equals(parentId)) {
            throw new AccessDeniedException("You do not have access to this account.");
        }
        return account;
    }

    private void assertAccountActive(SavingsAccount account) {
        if (account.getStatus() == SavingsAccount.Status.CLOSED) {
            throw new AccountClosedException();
        }
    }

    private Transaction buildTransaction(Long accountId, Transaction.Type type,
                                         BigDecimal amount, BigDecimal balanceAfter,
                                         String note, Long createdByUserId, String yearMonth) {
        Transaction tx = new Transaction();
        tx.setAccountId(accountId);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setNote(note);
        tx.setCreatedByUserId(createdByUserId);
        tx.setYearMonth(yearMonth);
        tx.setCreatedAt(LocalDateTime.now());
        return tx;
    }
}
