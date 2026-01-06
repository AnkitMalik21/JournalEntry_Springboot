package com.pro.Journal_Entry.dto;

import com.pro.Journal_Entry.enums.Mood;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class JournalRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 100,message = "Title cannot exceed 100 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Journal date is required")
    private LocalDate journalDate;

    private Mood mood;
}
