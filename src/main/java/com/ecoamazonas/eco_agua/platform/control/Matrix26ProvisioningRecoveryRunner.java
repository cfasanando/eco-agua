package com.ecoamazonas.eco_agua.platform.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ProvisioningRecoveryRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Matrix26ProvisioningRecoveryRunner.class);
    private static final String INTERRUPTION_MESSAGE =
            "La ejecución fue interrumpida por un reinicio o cierre del Control Center. Revisa el último paso y reintenta el plan.";

    private final Matrix26ProvisioningJobRepository jobRepository;
    private final Matrix26ProvisioningStepRepository stepRepository;
    private final Matrix26InstanceAuditLogRepository auditRepository;

    public Matrix26ProvisioningRecoveryRunner(
            Matrix26ProvisioningJobRepository jobRepository,
            Matrix26ProvisioningStepRepository stepRepository,
            Matrix26InstanceAuditLogRepository auditRepository
    ) {
        this.jobRepository = jobRepository;
        this.stepRepository = stepRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Matrix26ProvisioningJob> interruptedJobs =
                jobRepository.findByStatusOrderByUpdatedAtAsc("RUNNING");
        for (Matrix26ProvisioningJob job : interruptedJobs) {
            recoverJob(job);
        }
        if (!interruptedJobs.isEmpty()) {
            LOGGER.warn("Recovered {} interrupted Matrix26 provisioning job(s).", interruptedJobs.size());
        }
    }

    private void recoverJob(Matrix26ProvisioningJob job) {
        LocalDateTime now = LocalDateTime.now();
        for (Matrix26ProvisioningStep step : stepRepository.findByJob_IdOrderByDisplayOrderAscIdAsc(job.getId())) {
            if (!"RUNNING".equals(step.getStatus())) {
                continue;
            }
            step.setStatus("FAILED");
            step.setLastError(INTERRUPTION_MESSAGE);
            step.setDetail("Paso interrumpido. La ejecución puede reanudarse de forma idempotente.");
            step.setCompletedAt(now);
            stepRepository.save(step);
        }

        job.setStatus("FAILED");
        job.setLastError(INTERRUPTION_MESSAGE);
        job.setExecutionCompletedAt(now);
        jobRepository.save(job);

        Matrix26InstanceAuditLog log = new Matrix26InstanceAuditLog();
        log.setAction("PROVISIONING_EXECUTION_INTERRUPTED");
        log.setActorUsername("system");
        log.setSummary("Se recuperó como FALLIDO el aprovisionamiento interrumpido " + job.getReferenceCode() + ".");
        log.setAfterSnapshot("reference=" + job.getReferenceCode()
                + ";instanceCode=" + job.getInstanceCode()
                + ";status=FAILED");
        auditRepository.save(log);
    }
}
