package com.aztu.support.repository;

import com.aztu.support.domain.User;
import com.aztu.support.domain.enums.AccountStatus;
import com.aztu.support.domain.enums.RoleName;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByStatus(AccountStatus status, Pageable pageable);

    long countByStatus(AccountStatus status);

    List<User> findByRole_NameIn(List<RoleName> names);

    List<User> findByRole_NameInAndStatus(List<RoleName> names, AccountStatus status);

    Page<User> findByRole_Name(RoleName name, Pageable pageable);
}
