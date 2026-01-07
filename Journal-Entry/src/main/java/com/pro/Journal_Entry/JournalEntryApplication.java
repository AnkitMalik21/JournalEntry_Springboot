package com.pro.Journal_Entry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class JournalEntryApplication {

	public static void main(String[] args) {
		SpringApplication.run(JournalEntryApplication.class, args);
	}

}
