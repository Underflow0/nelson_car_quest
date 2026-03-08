package org.nelson.kidbank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nelson.kidbank.entity.*;
import org.nelson.kidbank.exception.*;
import org.nelson.kidbank.repository.*;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock SavingsAccountRepository accountRepo;
    @Mock TransactionRepository txRepo;
    @Mock UserRepository userRepo;

    @InjectMocks AccountService accountService;

    private SavingsAccount activeAccount;

    @BeforeEach
    void setUp() {
        activeAccount = new SavingsAccount();
        activeAccount.setId(1L);
        activeAccount.setChildUserId(10L);
        activeAccount.setParentUserId(1L);
        activeAccount.setBalance(new BigDecimal("1000.00"));
        activeAccount.setInterestRateAnnual(new BigDecimal("0.0500"));
        activeAccount.setStatus(SavingsAccount.Status.ACTIVE);
        activeAccount.setVersion(0L);
    }

    // --- Deposit ---

    @Test
    void deposit_increasesBalanceAndCreatesLedgerEntry() {
        when(accountRepo.findById(1L)).thenReturn(Optional.of(activeAccount));
        when(accountRepo.save(any())).thenReturn(activeAccount);
        when(txRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction tx = accountService.deposit(1L, new BigDecimal("50.00"), "Birthday money", 1L);

        assertThat(activeAccount.getBalance()).isEqualByComparingTo("1050.00");
        assertThat(tx.getType()).isEqualTo(Transaction.Type.DEPOSIT);
        assertThat(tx.getAmount()).isEqualByComparingTo("50.00");
        assertThat(tx.getBalanceAfter()).isEqualByComparingTo("1050.00");
        verify(txRepo).save(any(Transaction.class));
    }

    // --- Withdrawal ---

    @Test
    void withdraw_decreasesBalanceAndCreatesLedgerEntry() {
        when(accountRepo.findById(1L)).thenReturn(Optional.of(activeAccount));
        when(accountRepo.save(any())).thenReturn(activeAccount);
        when(txRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction tx = accountService.withdraw(1L, new BigDecimal("100.00"), "Toy", 1L);

        assertThat(activeAccount.getBalance()).isEqualByComparingTo("900.00");
        assertThat(tx.getType()).isEqualTo(Transaction.Type.WITHDRAWAL);
        assertThat(tx.getAmount()).isEqualByComparingTo("100.00");
        assertThat(tx.getBalanceAfter()).isEqualByComparingTo("900.00");
        verify(txRepo).save(any(Transaction.class));
    }

    @Test
    void withdraw_insufficientFunds_throwsAndCreatesNoLedgerEntry() {
        when(accountRepo.findById(1L)).thenReturn(Optional.of(activeAccount));

        assertThatThrownBy(() -> accountService.withdraw(1L, new BigDecimal("9999.00"), null, 1L))
                .isInstanceOf(InsufficientFundsException.class);

        verify(txRepo, never()).save(any());
    }

    // --- Closed account ---

    @Test
    void deposit_toClosedAccount_throwsAccountClosedException() {
        activeAccount.setStatus(SavingsAccount.Status.CLOSED);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(activeAccount));

        assertThatThrownBy(() -> accountService.deposit(1L, BigDecimal.TEN, null, 1L))
                .isInstanceOf(AccountClosedException.class);
    }

    @Test
    void withdraw_fromClosedAccount_throwsAccountClosedException() {
        activeAccount.setStatus(SavingsAccount.Status.CLOSED);
        when(accountRepo.findById(1L)).thenReturn(Optional.of(activeAccount));

        assertThatThrownBy(() -> accountService.withdraw(1L, BigDecimal.TEN, null, 1L))
                .isInstanceOf(AccountClosedException.class);
    }

    // --- Duplicate account ---

    @Test
    void createAccount_whenAlreadyExists_throwsAccountAlreadyExistsException() {
        when(accountRepo.existsByChildUserId(10L)).thenReturn(true);

        assertThatThrownBy(() -> accountService.createAccount(10L, 1L, BigDecimal.ZERO))
                .isInstanceOf(AccountAlreadyExistsException.class);

        verify(accountRepo, never()).save(any());
    }
}
