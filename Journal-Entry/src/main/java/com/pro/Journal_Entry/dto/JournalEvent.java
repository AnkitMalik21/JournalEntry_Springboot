package com.pro.Journal_Entry.dto;

import com.pro.Journal_Entry.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Kafka Event Message
 * This will be sent to Kafka whenever a journal is created/updated/deleted
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEvent {
    private EventType eventType;
    private Long journalId;
    private Long userId;
    private String username;
    private LocalDate journalDate;
    private String title;
    private LocalDateTime timestamp;
}
