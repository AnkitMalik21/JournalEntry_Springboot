package com.pro.Journal_Entry.service;

import com.pro.Journal_Entry.dto.JournalEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaConsumerService {
/**
 * @KafkaListener - Automatically consumes messages from topic
 *
 * This method runs in background whenever message arrives
 */
     @KafkaListener(
             topics = "${kafka.topic.journal-events}",
             groupId = "${spring.kafka.consumer.group-id}"
     )
    public void consumerJournalEvent(JournalEvent event){

         log.info("==========================================");
         log.info("Kafka Event Received:");
         log.info("Event Type: {}", event.getEventType());
         log.info("Journal ID: {}", event.getJournalId());
         log.info("User: {} (ID: {})", event.getUsername(), event.getUserId());
         log.info("Journal Date: {}", event.getJournalDate());
         log.info("Title: {}", event.getTitle());
         log.info("Timestamp: {}", event.getTimestamp());
         log.info("==========================================");

         //Process based on event type
         switch(event.getEventType()){
             case JOURNAL_CREATED :
                 handleJournalCreated(event);
                 break;

             case JOURNAL_UPDATED:
                 handleJournalUpdated(event);
                 break;

             case JOURNAL_DELETED:
                 handleJournalDeleted(event);
                 break;
         }
     }

    /**
     * Handle journal created event
     * Future: Send welcome email, update analytics
     */

    private void handleJournalCreated(JournalEvent event){
        log.info("Processing JOURNAL_CREATED for user: {}",event.getUsername());
        // TODO: Send email notification
        // emailService.sendNewJournalNotification(event);

        // TODO: Update analytics
        // analyticsService.trackJournalCreated(event);

        // TODO: AI mood analysis
        // aiService.analyzeMood(event.getJournalId());
    }

    private void handleJournalUpdated(JournalEvent event){
        log.info("Processing JOURNAL_UPDATED for journal: {}",event.getJournalId());
        //TODO: LOG ACTIVITY
        //
    }

    private void handleJournalDeleted(JournalEvent event){
        log.info("Processing JOURNAL_DELETED for journal: {}",event.getJournalId());
        //Todo : archive Journal
        //archiveService.archiveJournal(event.getJournalId());
    }
}
