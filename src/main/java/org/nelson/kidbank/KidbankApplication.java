package org.nelson.kidbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KidbankApplication {
    public static void main(String[] args) {
        SpringApplication.run(KidbankApplication.class, args);
    }
}
