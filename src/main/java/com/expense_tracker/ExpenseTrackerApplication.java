package com.expense_tracker;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

//@EnableCaching
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class ExpenseTrackerApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        // Set values as system properties so Spring Boot can read ${VAR_NAME}
        System.setProperty("DB_USERNAME", dotenv.get("DB_USERNAME"));
        System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));
        System.setProperty("SMTP_USERNAME", dotenv.get("SMTP_USERNAME"));
        System.setProperty("SMTP_PASSWORD", dotenv.get("SMTP_PASSWORD"));

        SpringApplication.run(ExpenseTrackerApplication.class, args);
    }

}