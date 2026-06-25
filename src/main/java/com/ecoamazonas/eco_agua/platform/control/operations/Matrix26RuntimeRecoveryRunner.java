package com.ecoamazonas.eco_agua.platform.control.operations;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26RuntimeRecoveryRunner implements ApplicationRunner {

    private final Matrix26RuntimeControlService runtimeControlService;

    public Matrix26RuntimeRecoveryRunner(Matrix26RuntimeControlService runtimeControlService) {
        this.runtimeControlService = runtimeControlService;
    }

    @Override
    public void run(ApplicationArguments args) {
        runtimeControlService.recoverInterruptedOperations();
    }
}
