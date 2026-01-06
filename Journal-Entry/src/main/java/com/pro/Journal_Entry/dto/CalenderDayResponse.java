package com.pro.Journal_Entry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalenderDayResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate date;
    private Boolean hasJournal;
    private Long journalId;
    private String title;
}


