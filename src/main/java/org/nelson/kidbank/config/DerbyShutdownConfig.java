package org.nelson.kidbank.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.DriverManager;

/**
 * Issues the Derby embedded shutdown command on application stop.
 * Derby always throws SQLNonTransientConnectionException on shutdown — this is normal and expected.
 */
@Component
public class DerbyShutdownConfig {

    private static final Logger log = LoggerFactory.getLogger(DerbyShutdownConfig.class);

    @PreDestroy
    public void shutdownDerby() {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (java.sql.SQLException e) {
            // Derby always throws on shutdown. SQLState "XJ015" = clean system shutdown.
            if ("XJ015".equals(e.getSQLState()) || "08006".equals(e.getSQLState())) {
                log.info("Apache Derby shut down cleanly.");
            } else {
                log.warn("Unexpected exception during Derby shutdown: {} (SQLState={})",
                        e.getMessage(), e.getSQLState());
            }
        }
    }
}
