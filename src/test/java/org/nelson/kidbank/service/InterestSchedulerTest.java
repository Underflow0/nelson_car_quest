package org.nelson.kidbank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nelson.kidbank.entity.*;
import org.nelson.kidbank.repository.*;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterestSchedulerTest {

    @Mock SavingsAccountRepository accountRepo;
    @Mock TransactionRepository txRepo;

    @InjectMocks InterestSchedulerService scheduler;

    private SavingsAccount account;

    @BeforeEach
    void setUp() {
        account = new SavingsAccount();
        account.setId(1L);
        account.setBalance(new BigDecimal("1000.00"));
        account.setInterestRateAnnual(new BigDecimal("0.0500"));
        account.setStatus(SavingsAccount.Status.ACTIVE);
        account.setLastInterestAppliedMonth(null);
        account.setVersion(0L);
    }

    @Test
    void interestCalculation_1000at5percent_produces4dot17() {
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepo.save(any())).thenReturn(account);
        when(txRepo.findByAccountIdAndTypeAndYearMonth(any(), any(), any())).thenReturn(Optional.empty());
        when(txRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.applyInterestToAccount(1L, "2025-01");

        // $1000 * 0.05 / 12 = $4.1666... → rounded to $4.17
        assertThat(account.getBalance()).isEqualByComparingTo("1004.17");

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(txRepo).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getAmount()).isEqualByComparingTo("4.17");
        assertThat(txCaptor.getValue().getType()).isEqualTo(Transaction.Type.INTEREST);
        assertThat(txCaptor.getValue().getYearMonth()).isEqualTo("2025-01");
    }

    @Test
    void interest_notAppliedTwiceForSameMonth_appLevelCheck() {
        account.setLastInterestAppliedMonth("2025-01");
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));

        scheduler.applyInterestToAccount(1L, "2025-01");

        verify(txRepo, never()).save(any());
        assertThat(account.getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void interest_notAppliedTwiceForSameMonth_dbLevelCheck() {
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));
        Transaction existing = new Transaction();
        when(txRepo.findByAccountIdAndTypeAndYearMonth(1L, Transaction.Type.INTEREST, "2025-01"))
                .thenReturn(Optional.of(existing));

        scheduler.applyInterestToAccount(1L, "2025-01");

        verify(txRepo, never()).save(any());
    }

    @Test
    void interest_skipsClosedAccount() {
        account.setStatus(SavingsAccount.Status.CLOSED);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(account));
        when(txRepo.findByAccountIdAndTypeAndYearMonth(any(), any(), any())).thenReturn(Optional.empty());

        scheduler.applyInterestToAccount(1L, "2025-01");

        verify(txRepo, never()).save(any());
    }
}
