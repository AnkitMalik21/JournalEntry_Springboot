package com.pro.Journal_Entry.entity;

import com.pro.Journal_Entry.enums.Mood;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "journal_entries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","journal_date"})
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JournalEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,length=100)
    private String title;

    @Column(nullable = false,columnDefinition = "TEXT")
    private String content;

    /**
     * Journal Date - The date this journal entry is for
     * Not when it was created, but which day it describes
     */

    @Column(name="journal_date",nullable = false)
    private LocalDate journalDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Mood mood;

    /**
     * @ManyToOne - Many journals belong to one user
     * Think: John can write many journals, but each journal belongs to only John
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @Column(nullable = false)
    private Boolean deleted = false; //soft delete

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
