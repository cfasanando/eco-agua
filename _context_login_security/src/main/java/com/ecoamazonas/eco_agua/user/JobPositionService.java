package com.ecoamazonas.eco_agua.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class JobPositionService {

    private final JobPositionRepository repository;

    public JobPositionService(JobPositionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<JobPosition> findAll() {
        return repository.findAllOrdered();
    }

    @Transactional(readOnly = true)
    public List<JobPosition> findActive() {
        return repository.findActive();
    }

    @Transactional(readOnly = true)
    public JobPositionForm buildForm(Long id) {
        if (id == null) {
            return new JobPositionForm();
        }

        JobPosition position = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job position not found: " + id));

        JobPositionForm form = new JobPositionForm();
        form.setId(position.getId());
        form.setName(position.getName());
        form.setDescription(position.getDescription());
        form.setBaseSalary(position.getBaseSalary());
        form.setSalaryPeriod(position.getSalaryPeriod());
        form.setActive(position.isActive());
        form.setPaymentMode(position.getPaymentMode());
        form.setSalesCommissionPercent(position.getCommissionRate());

        return form;
    }

    @Transactional
    public JobPosition saveFromForm(JobPositionForm form) {
        JobPosition position;

        if (form.getId() != null) {
            position = repository.findById(form.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Job position not found: " + form.getId()));
        } else {
            position = new JobPosition();
        }

        position.setName(form.getName());
        position.setDescription(form.getDescription());
        position.setBaseSalary(
                form.getBaseSalary() != null ? form.getBaseSalary() : BigDecimal.ZERO
        );
        position.setSalaryPeriod(
                form.getSalaryPeriod() != null ? form.getSalaryPeriod() : SalaryPeriod.MONTHLY
        );
        position.setActive(form.isActive());

        PaymentMode mode = form.getPaymentMode() != null ? form.getPaymentMode() : PaymentMode.FIXED;
        position.setPaymentMode(mode);

        BigDecimal commission =
                form.getSalesCommissionPercent() != null ? form.getSalesCommissionPercent() : BigDecimal.ZERO;
        position.setCommissionRate(commission);

        // opcional: si quieres reflejar algo en salaryAmount
        position.setSalaryAmount(position.getBaseSalary());

        return repository.save(position);
    }

    @Transactional
    public void deleteSoft(Long id) {
        JobPosition position = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job position not found: " + id));
        position.setActive(false);
        repository.save(position);
    }
}
