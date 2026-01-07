package com.pro.Journal_Entry.service;

import com.pro.Journal_Entry.dto.CalendarDayResponse;  // ← FIXED: Was CalenderDayResponse
import com.pro.Journal_Entry.dto.JournalRequest;
import com.pro.Journal_Entry.dto.JournalResponse;
import com.pro.Journal_Entry.entity.JournalEntry;
import com.pro.Journal_Entry.entity.User;
import com.pro.Journal_Entry.enums.EventType;
import com.pro.Journal_Entry.exception.DuplicateJournalException;
import com.pro.Journal_Entry.exception.ResourceNotFoundException;  // ← FIXED: Wrong import
import com.pro.Journal_Entry.repository.JournalRepository;
import com.pro.Journal_Entry.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;  // ← FIXED: Wrong import

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Journal Service - Core business logic
 *
 * CACHING STRATEGY:
 * - @Cacheable - Check cache first, if not found, execute method and cache result
 * - @CacheEvict - Remove from cache when data changes
 * - @Caching - Combine multiple cache operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JournalService {

    private final JournalRepository journalRepository;
    private final UserRepository userRepository;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Create journal entry
     *
     * FLOW:
     * 1. Validate user exists
     * 2. Check duplicate for same date
     * 3. Save to database
     * 4. Send Kafka event
     * 5. Return response
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "journals", key = "#userId + '_' + #request.journalDate"),
            @CacheEvict(value = "calendar", key = "#userId + '_' + #request.journalDate.year + '_' + #request.journalDate.monthValue")
    })
    public JournalResponse createJournal(Long userId, JournalRequest request) {

        // FIXED: Changed to existsByUserIdAndJournalDateAndDeletedFalse
        if (journalRepository.existsByUserIdAndJournalDateAndDeletedFalse(userId, request.getJournalDate())) {
            throw new DuplicateJournalException("Journal already exists for date: " + request.getJournalDate());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        JournalEntry journal = JournalEntry.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .journalDate(request.getJournalDate())
                .mood(request.getMood())
                .user(user)
                .deleted(false)
                .build();

        journal = journalRepository.save(journal);

        kafkaProducerService.sendJournalEvent(journal, EventType.JOURNAL_CREATED);

        log.info("Journal created: userId={}, date={}", userId, request.getJournalDate());
        return mapToResponse(journal);
    }

    /**
     * Get journal by date
     *
     * @Cacheable - Result cached with key "userId_date"
     * Example: "123_2026-01-05"
     */
    @Cacheable(value = "journals", key = "#userId + '_' + #date")
    public JournalResponse getJournalByDate(Long userId, LocalDate date) {

        // FIXED: Removed extra "JournalEntry" at the end
        JournalEntry journal = journalRepository
                .findByUserIdAndJournalDateAndDeletedFalse(userId, date)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No journal found for date: " + date
                ));

        log.info("Journal retrieved from database: userId={}, date={}", userId, date);
        return mapToResponse(journal);
    }

    /**
     * Get user's journals (paginated)
     */
    public Page<JournalResponse> getUserJournals(Long userId, Pageable pageable) {
        // FIXED: Changed to findByUserIdAndDeletedFalse
        Page<JournalEntry> journals = journalRepository.findByUserIdAndDeletedFalse(userId, pageable);
        return journals.map(this::mapToResponse);
    }

    /**
     * Get calendar view for month
     * Shows which days have journals
     *
     * Example: GET /api/journals/calendar?month=2026-01
     */
    @Cacheable(value = "calendar", key = "#userId + '_' + #yearMonth.year + '_' + #yearMonth.monthValue")
    public List<CalendarDayResponse> getCalendarMonth(Long userId, YearMonth yearMonth) {  // FIXED: Method name

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<JournalEntry> journals = journalRepository
                .findByUserIdAndDateRange(userId, startDate, endDate);

        List<CalendarDayResponse> calendar = new ArrayList<>();  // FIXED: Variable name

        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate currentDate = yearMonth.atDay(day);

            JournalEntry journal = journals.stream()
                    .filter(j -> j.getJournalDate().equals(currentDate))
                    .findFirst()
                    .orElse(null);

            calendar.add(CalendarDayResponse.builder()
                    .date(currentDate)
                    .hasJournal(journal != null)
                    .journalId(journal != null ? journal.getId() : null)
                    .title(journal != null ? journal.getTitle() : null)  // FIXED: Was getContent()
                    .build());
        }

        log.info("Calendar retrieved: userId={}, month={}", userId, yearMonth);
        return calendar;
    }

    /**
     * Update journal
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "journals", key = "#userId + '_' + #result.journalDate"),
            @CacheEvict(value = "calendar", key = "#userId + '_' + #result.journalDate.year + '_' + #result.journalDate.monthValue")
    })
    public JournalResponse updateJournal(Long userId, Long journalId, JournalRequest request) {

        JournalEntry journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found"));

        if (!journal.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        journal.setTitle(request.getTitle());
        journal.setContent(request.getContent());
        journal.setMood(request.getMood());

        journal = journalRepository.save(journal);

        kafkaProducerService.sendJournalEvent(journal, EventType.JOURNAL_UPDATED);

        log.info("Journal updated: id={}", journalId);
        return mapToResponse(journal);
    }

    /**
     * Delete journal (soft delete)
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "journals", allEntries = true),
            @CacheEvict(value = "calendar", allEntries = true)
    })
    public void deleteJournal(Long userId, Long journalId) {

        JournalEntry journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found"));

        if (!journal.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        journal.setDeleted(true);
        journalRepository.save(journal);

        kafkaProducerService.sendJournalEvent(journal, EventType.JOURNAL_DELETED);

        log.info("Journal deleted: id={}", journalId);
    }

    /**
     * Search journals by keyword
     */
    public Page<JournalResponse> searchJournals(Long userId, String keyword, Pageable pageable) {
        Page<JournalEntry> journals = journalRepository
                .searchJournals(userId, keyword, pageable);
        return journals.map(this::mapToResponse);
    }

    /**
     * Admin: Get all journals
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Page<JournalResponse> getAllJournals(Pageable pageable) {
        Page<JournalEntry> journals = journalRepository.findByDeletedFalse(pageable);
        return journals.map(this::mapToResponse);
    }

    /**
     * Admin: Delete any journal
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "journals", allEntries = true),
            @CacheEvict(value = "calendar", allEntries = true)
    })
    public void adminDeleteJournal(Long journalId) {
        JournalEntry journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found"));

        journal.setDeleted(true);
        journalRepository.save(journal);

        kafkaProducerService.sendJournalEvent(journal, EventType.JOURNAL_DELETED);

        log.info("Admin deleted journal: id={}", journalId);
    }

    /**
     * Map entity to DTO
     */
    private JournalResponse mapToResponse(JournalEntry journal) {
        return JournalResponse.builder()
                .id(journal.getId())
                .title(journal.getTitle())
                .content(journal.getContent())
                .journalDate(journal.getJournalDate())
                .mood(journal.getMood())
                .userId(journal.getUser().getId())
                .username(journal.getUser().getUsername())
                .createdAt(journal.getCreatedAt())
                .updatedAt(journal.getUpdatedAt())
                .build();
    }
}
