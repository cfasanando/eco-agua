package com.ecoamazonas.eco_agua.platform.control.backups;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        prefix = "matrix26.control-center",
        name = {"enabled", "backups.scheduling-enabled"},
        havingValue = "true"
)
public class Matrix26BackupSchedulingConfiguration {
}
