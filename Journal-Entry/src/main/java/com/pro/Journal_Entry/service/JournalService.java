package com.pro.Journal_Entry.service;

import com.pro.Journal_Entry.dto.CalenderDayResponse;
import com.pro.Journal_Entry.dto.JournalRequest;
import com.pro.Journal_Entry.dto.JournalResponse;
import com.pro.Journal_Entry.entity.JournalEntry;
import com.pro.Journal_Entry.entity.User;
import com.pro.Journal_Entry.enums.EventType;
import com.pro.Journal_Entry.exception.DuplicateJournalException;
import com.pro.Journal_Entry.repository.JournalRepository;
import com.pro.Journal_Entry.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
@Slf4j //Lombok: provides logger
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
    @Caching(evict ={
            @CacheEvict(value = "journals",key = "#userId +'-' + #request.journalDate"),
            @CacheEvict(value = "calender", key = "#userId + '-' + #request.journalDate.year + '-' + #request.journalDate.monthValue")
    })
    public JournalResponse createJournal(Long userId, JournalRequest request){
        //Check if the journal already exists for this date
        if(journalRepository.existsByUserIdAndJournalDateAndDeleteFalse(userId,request.getJournalDate())){
            throw new DuplicateJournalException("Journal already exists for date: " + request.getJournalDate());
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        //Create journal entry
        JournalEntry journal = JournalEntry.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .journalDate(request.getJournalDate())
                .mood(request.getMood())
                .user(user)
                .deleted(false)
                .build();

        journal = journalRepository.save(journal);

        //Send kafka event (asynchronous)
        kafkaProducerService.sendJournalEvent(journal, EventType.JOURNAL_CREATED);

        log.info("Journal created: userId={},date={}",userId,request.getJournalDate());
        return mapToResponse(journal);
    }

    /**
     * Get journal by date
     *
     * @Cacheable - Result cached with key "userId_date"
     * Example: "123_2026-01-05"
     */

    @Cacheable(value = "journals",key="#userId + '_' + #date")
    public JournalResponse getJournalByDate(Long userId, LocalDate date){
        JournalEntry journal = journalRepository
                .findByUserIdAndJournalDateAndDeletedFalseJournalEntry(userId,date)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No journal found for date: " + date
                ));
        log.info("Journal retrieved from database: userId={}, date={}",userId,date);
        return mapToResponse(journal);
    }

    /**
     * Get user's journals (paginated)
     */
    public Page<JournalResponse> getUserJournals(Long userId, Pageable pageable){
        Page<JournalEntry> journals = journalRepository.findByUserIdAndDeleteFalse(userId,pageable);
        return journals.map(this::mapToResponse);
    }

    /**
     * Get calendar view for month
     * Shows which days have journals
     *
     * Example: GET /api/journals/calendar?month=2026-01
     */

    @Cacheable(value = "calendar", key ="#userId + '_' + #yearMonth.year + '_' + #yearMonth.monthValue")
    public List<CalenderDayResponse> getCalenderMonth(Long userId, YearMonth yearMonth){
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        //Get all journals for this month
        List<JournalEntry> journals = journalRepository
                .findByUserIdAndDateRange(userId,startDate,endDate);

        //create response for each day in month
        List<CalenderDayResponse> calender = new ArrayList<>();

        for(int day = 1;day<=yearMonth.lengthOfMonth();day++){
            LocalDate currentDate = yearMonth.atDay(day);

            //Find journal for this day
            JournalEntry journal = journals.stream()
                    .filter(j->j.getJournalDate().equals(currentDate))
                    .findFirst()
                    .orElse(null);

            calender.add(CalenderDayResponse.builder()
                    .date(currentDate)
                    .hasJournal(journal!=null)
                    .journalId(journal != null ? journal.getId() : null)
                    .title(journal != null ? journal.getContent() : null)
                    .build());
        }
        log.info("Calender retrieved: userId={}, month={}",userId,yearMonth);
        return calender;
    }

    // update journal
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "journals",key = "#userId + '_' + #result.journalDate"),
            @CacheEvict(value = "calender", key = "#userId + '_' + #result.journalDate.year + '_' + #result.journalDate.monthValue")
    })
    public JournalResponse updateJournal(Long userId,Long journalId,JournalRequest request){
        JournalEntry journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found"));

        //check ownership
        if(!journal.getUser().getId().equals(userId)){
            throw new RuntimeException("Unauthorized");
        }

        journal.setTitle(request.getTitle());
        journal.setContent(request.getContent());
        journal.setMood(request.getMood());

        journal = journalRepository.save(journal);

        //Send Kafka event
        kafkaProducerService.sendJournalEvent(journal,EventType.JOURNAL_UPDATED);

        log.info("Journal updated: id={}",journalId);
        return mapToResponse(journal);
    }

    //Delete journal (soft delete)
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "journals",allEntries = true),
            @CacheEvict(value = "calender",allEntries = true)
    })
    public void deleteJournal(Long userId,Long journalId){
        JournalEntry journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found"));

        //check ownership
        if(!journal.getUser().getId().equals(userId)){
            throw new RuntimeException("Unauthorized");
        }

        //Soft delete
        journal.setDeleted(true);
        journalRepository.save(journal);

        //Send Kafka event
        kafkaProducerService.sendJournalEvent(journal,EventType.JOURNAL_DELETED);
        log.info("Journal deleted: id={}",journalId);
    }
}
