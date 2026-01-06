package com.pro.Journal_Entry.config;


import com.pro.Journal_Entry.dto.JournalEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer Configuration
 *
 * WHAT IS KAFKA?
 * Think of Kafka as a post office:
 * - Producer = Person sending letter
 * - Topic = Mailbox
 * - Consumer = Person receiving letter
 *
 * WHY KAFKA?
 * - Asynchronous processing - Don't wait for background tasks
 * - Decoupling - Journal service doesn't know who uses events
 * - Scalability - Multiple consumers can process events
 */

@Configuration
public class KafkaProducerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    //producer Factory configuration
    @Bean
    public ProducerFactory<String, JournalEvent> producerFactory(){
        Map<String,Object> config = new HashMap<>();

        //kafka server address
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);

        //key serializer - converts key to bytes
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        //value Serializer - converts JournalEvent to JSON
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        //producer reliability setting
        config.put(ProducerConfig.ACKS_CONFIG,"all"); //wait for all replicas
        config.put(ProducerConfig.RETRIES_CONFIG,3);  // Retry 3 time on failure

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Kafka Template - Used to send messages
     */

    @Bean
    public KafkaTemplate<String,JournalEvent> kafkaTemplate(){
        return new KafkaTemplate<>(producerFactory());
    }
}
