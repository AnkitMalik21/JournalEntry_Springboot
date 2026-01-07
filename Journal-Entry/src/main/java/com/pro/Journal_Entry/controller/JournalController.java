package com.pro.Journal_Entry.controller;

import com.pro.Journal_Entry.dto.CalendarDayResponse;
import com.pro.Journal_Entry.dto.JournalRequest;
import com.pro.Journal_Entry.dto.JournalResponse;
import com.pro.Journal_Entry.security.JwtUtil;
import com.pro.Journal_Entry.service.JournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;
    private final JwtUtil jwtUtil;

    /**
     * Extract userId from JWT token
     */
    private Long getUserIdFromToken(String token) {
        return jwtUtil.extractUserId(token.substring(7));
    }

    /**
     * Create journal entry
     * POST /api/journals
     */
    @PostMapping
    public ResponseEntity<JournalResponse> createJournal(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody JournalRequest request
    ) {
        Long userId = getUserIdFromToken(token);
        JournalResponse response = journalService.createJournal(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get journal by date
     * GET /api/journals/date/2026-01-05
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<JournalResponse> getJournalByDate(
            @RequestHeader("Authorization") String token,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = getUserIdFromToken(token);
        JournalResponse response = journalService.getJournalByDate(userId, date);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's journals (paginated)
     * GET /api/journals?page=0&size=10&sort=journalDate,desc
     */
    @GetMapping
    public ResponseEntity<Page<JournalResponse>> getUserJournals(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "journalDate,desc") String[] sort
    ) {
        Long userId = getUserIdFromToken(token);

        // Create Pageable with sorting
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.fromString(sort[1]), sort[0])
        );

        Page<JournalResponse> response = journalService.getUserJournals(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get calendar month view
     * GET /api/journals/calendar?month=2026-01
     */
    @GetMapping("/calendar")
    public ResponseEntity<List<CalendarDayResponse>> getCalendarMonth(
            @RequestHeader("Authorization") String token,  // ← FIXED: Added token parameter
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        Long userId = getUserIdFromToken(token);  // ← FIXED: Changed from getCurrentUserId()

        List<CalendarDayResponse> calendar = journalService.getCalendarMonth(userId, month);

        return ResponseEntity.ok(calendar);
    }

    /**
     * Update journal
     * PUT /api/journals/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<JournalResponse> updateJournal(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @Valid @RequestBody JournalRequest request
    ) {
        Long userId = getUserIdFromToken(token);
        JournalResponse response = journalService.updateJournal(userId, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete journal
     * DELETE /api/journals/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJournal(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id
    ) {
        Long userId = getUserIdFromToken(token);
        journalService.deleteJournal(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Search journals by keyword
     * GET /api/journals/search?keyword=happy&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<Page<JournalResponse>> searchJournals(
            @RequestHeader("Authorization") String token,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = getUserIdFromToken(token);
        Pageable pageable = PageRequest.of(page, size, Sort.by("journalDate").descending());

        Page<JournalResponse> response = journalService.searchJournals(userId, keyword, pageable);
        return ResponseEntity.ok(response);
    }

    // ===== ADMIN ENDPOINTS =====

    /**
     * Admin: Get all journals
     * GET /api/journals/admin/all
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<JournalResponse>> getAllJournals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<JournalResponse> response = journalService.getAllJournals(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin: Delete any journal
     * DELETE /api/journals/admin/{id}
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminDeleteJournal(@PathVariable Long id) {
        journalService.adminDeleteJournal(id);
        return ResponseEntity.noContent().build();
    }
}
