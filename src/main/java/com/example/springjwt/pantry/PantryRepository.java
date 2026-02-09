package com.example.springjwt.pantry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PantryRepository extends JpaRepository<Pantry, Long> {

    boolean existsByUser_IdAndNameIgnoreCase(int userId, String name);

    List<Pantry> findAllByUser_IdOrderBySortOrderAscCreatedAtAsc(int userId);

    Optional<Pantry> findByIdAndUser_Id(Long pantryId, int userId);

    Optional<Pantry> findFirstByUser_IdAndIsDefaultTrue(int userId);
    
}
