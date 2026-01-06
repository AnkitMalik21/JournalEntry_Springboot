package com.pro.Journal_Entry.dto;

import com.pro.Journal_Entry.enums.Mood;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Serializable - Required for Redis caching
 * Redis stores objects as bytes, so they must be Serializable
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String content;
    private LocalDate journalDate;
    private Mood mood;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

/**
 *Java: Understands complex Objects
 *(like your User class with id, username, roles, etc.).
 *These objects live in the computer's memory (RAM) in a specific structure.
 *Redis: Does not understand Java Objects at all.
 *It is a simple storage system that only understands Bytes (sequences of 0s and 1s) or plain text.
 */


/**
Serialization is the process of converting a complex Java Object into
a flat stream of bytes (0s and 1s) so it can be sent over a network or stored.
 */


/**
You will often see this line in Serializable classes. Think of this as a Version Control ID or a "Security Seal."

Scenario: You save a JournalResponse to Redis today.

Tomorrow: You change the Java code (e.g., you rename title to header).

The Problem: When Java tries to pull the old data from Redis, it looks at the new code and says, "Wait, these don't match!"

The serialVersionUID = 1L tells Java: "As long as this ID is 1L, trust me that these are the same class, even if I made small changes." It prevents errors when your code evolves but you still have old data in the cache.
 */
