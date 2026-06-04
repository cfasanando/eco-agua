package com.ecoamazonas.eco_agua.user;

import java.time.LocalTime;

public class EmployeeAttendanceItemForm {

    private Long employeeId;
    private EmployeeAttendanceStatus status = EmployeeAttendanceStatus.PRESENT;
    private LocalTime startTime;
    private LocalTime endTime;
    private String observation;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public EmployeeAttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(EmployeeAttendanceStatus status) {
        this.status = status;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }
}
