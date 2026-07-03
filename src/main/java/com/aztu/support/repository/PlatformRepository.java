package com.aztu.support.repository;

import com.aztu.support.domain.Platform;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformRepository extends JpaRepository<Platform, Long> {

    List<Platform> findAllByOrderByNameAsc();

    List<Platform> findByActiveTrueOrderByNameAsc();

    Optional<Platform> findFirstByAdditionalTrue();

    boolean existsByNameIgnoreCase(String name);
}
