package com.pro.Journal_Entry.repository;

import com.pro.Journal_Entry.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalRepository extends JpaRepository<JournalEntry,Long> {
    /*
     * Find journal by user and date
     * Returns Optional - may or may not exist
     */

    Optional<JournalEntry> findByUserIdAndJournalDateAndDeletedFalseJournalEntry(
            Long userId, LocalDate journalDate);

    /*
      Check  if journal exists for user on specific date
    */

    Boolean existsByUserIdAndJournalDateAndDeleteFalse(
            Long userId,
            LocalDate journalDate
    );

    /*
    Get all journals for a user (paginated)
    Pageable allows : page number , size, sorting
    */
    Page<JournalEntry> findByUserIdAndDeleteFalse(
            Long userId,
            Pageable pageable
    );

    /**
     * Get journals for a specific month
     * Between dates: first day and last day of month
     */
    @Query("SELECT j from JournalEntry j where j.user.id =: userId  " +
            "And j.journalDate BETWEEN :startDate AND : endDate " +
            "AND j.delete = false")
    List<JournalEntry> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /*
      j.user.id is a path that tells Hibernate to:
      Start at the JournalEntry object (j).
      Go to its User object (.user).
      Read the ID field inside that User object (.id).
    */

    /*
      Search journals by keyword int title or content
    */
    @Query("SELECT j FROM JournalEntry j WHERE j.user.id = :userId " +
            "AND j.deleted = false " +
            "AND (LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(j.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<JournalEntry> searchJournals(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * Admin: Get all journals (for admin role)
     */
    Page<JournalEntry> findByDeletedFalse(Pageable pageable);
}
