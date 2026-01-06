package com.pro.Journal_Entry.service;

import com.pro.Journal_Entry.dto.JournalEvent;
import com.pro.Journal_Entry.entity.JournalEntry;
import com.pro.Journal_Entry.enums.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    private final KafkaTemplate<String, JournalEvent> kafkaTemplate;

    @Value("${kafka.topic.journal-events}")
    private String topicName;

    /**
     * Send journal event to Kafka
     * ASYNCHRONOUS: This doesn't wait for Kafka
     * Main thread continues immediately
     */

    public void sendJournalEvent(JournalEntry journal, EventType eventType){
        JournalEvent event = JournalEvent.builder()
                .eventType(eventType)
                .journalId(journal.getId())
                .userId(journal.getUser().getId())
                .username(journal.getUser().getUsername())
                .journalDate(journal.getJournalDate())
                .title(journal.getTitle())
                .timestamp(LocalDateTime.now())
                .build();

        //Send to Kafka (asynchronous)
        CompletableFuture<SendResult<String,JournalEvent>> future = kafkaTemplate
                .send(topicName,String.valueOf(journal.getId()),event);

        //Handle success/failure
        future.whenComplete((result,ex)->{
            if(ex == null){
                log.info("Kafka event send: topic={},event={},offset={}",topicName,eventType,result.getRecordMetadata().offset());
            }else{
                log.error("Failed to send Kafka event: {}",ex.getMessage());
            }
        });
    }
}
