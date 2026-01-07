package com.pro.Journal_Entry.exception;

public class DuplicateJournalException  extends RuntimeException{
    public DuplicateJournalException(String message){
        super(message);
    }
}
