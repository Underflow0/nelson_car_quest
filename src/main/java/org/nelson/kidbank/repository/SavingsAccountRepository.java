package org.nelson.kidbank.repository;

import org.nelson.kidbank.entity.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long> {

    Optional<SavingsAccount> findByChildUserId(Long childUserId);

    boolean existsByChildUserId(Long childUserId);

    List<SavingsAccount> findByParentUserId(Long parentUserId);

    /** All ACTIVE accounts — used by the interest scheduler. */
    List<SavingsAccount> findByStatus(SavingsAccount.Status status);
}
