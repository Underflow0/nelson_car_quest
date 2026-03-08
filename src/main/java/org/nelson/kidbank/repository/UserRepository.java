package org.nelson.kidbank.repository;

import org.nelson.kidbank.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** All CHILD users whose parentId matches the given parent. */
    List<User> findByParentId(Long parentId);
}
