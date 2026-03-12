package org.nelson.kidbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.InputStream;
import java.util.Properties;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class KidbankApplication {
    public static void main(String[] args) {
        // Apply configured timezone before Spring (and Hibernate) initialize,
        // so LocalDateTime.now() uses the correct zone on UTC servers.
        Properties props = new Properties();
        try (InputStream is = KidbankApplication.class.getResourceAsStream("/application.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}
        String tz = props.getProperty("app.timezone", "").strip();
        if (!tz.isEmpty()) {
            TimeZone.setDefault(TimeZone.getTimeZone(tz));
        }

        SpringApplication.run(KidbankApplication.class, args);
    }
}
