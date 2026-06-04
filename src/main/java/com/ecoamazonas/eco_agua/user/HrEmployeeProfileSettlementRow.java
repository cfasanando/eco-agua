package com.ecoamazonas.eco_agua.user;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class HrEmployeeProfileSettlementRow {

    private Long settlementId;
    private Long obligationId;
    private LocalDate settlementDate;
    private BigDecimal appliedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private String obligationLabel;
    private String observation;

    public Long getSettlementId() {
        return settlementId;
    }

    public void setSettlementId(Long settlementId) {
        this.settlementId = settlementId;
    }

    public Long getObligationId() {
        return obligationId;
    }

    public void setObligationId(Long obligationId) {
        this.obligationId = obligationId;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public BigDecimal getAppliedAmount() {
        return appliedAmount;
    }

    public void setAppliedAmount(BigDecimal appliedAmount) {
        this.appliedAmount = normalizeMoney(appliedAmount);
    }

    public String getObligationLabel() {
        return obligationLabel;
    }

    public void setObligationLabel(String obligationLabel) {
        this.obligationLabel = obligationLabel;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
