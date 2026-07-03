package com.aztu.support.repository;

import com.aztu.support.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByPlatform_IdOrderByNameAsc(Long platformId);

    List<Category> findByPlatform_IdAndActiveTrueOrderByNameAsc(Long platformId);

    boolean existsByPlatform_IdAndNameIgnoreCase(Long platformId, String name);
}
