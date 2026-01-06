package com.pro.Journal_Entry.config;

import com.pro.Journal_Entry.dto.JournalEvent;
import com.pro.Journal_Entry.entity.JournalEntry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer Configuration
 *
 * Consumer listens to topics and processes messages
 */
@Configuration
@EnableKafka
public class kafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Consumer Factory Configuration
     */
    @Bean
    public ConsumerFactory<String, JournalEvent> consumerFactory(){
        Map<String,Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG,groupId);

        //Deserializers - convert bytes back to objects
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        //Trust all packages for JSON deserialization
        config.put(JsonDeserializer.TRUSTED_PACKAGES,"*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.pro.Journal_Entry.dto.JournalEvent");


        //Start reading from the earliest message if no offset found
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new JsonDeserializer<>(JournalEvent.class,false)
        );
    }

    /**
     * Kafka Listener Container Factory
     * Creates listener containers for @KafkaListener methods
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String,JournalEvent> kafkaListenerContainerFactory(){
        ConcurrentKafkaListenerContainerFactory<String,JournalEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
