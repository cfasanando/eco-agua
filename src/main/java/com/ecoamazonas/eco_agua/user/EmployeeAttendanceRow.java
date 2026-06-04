package com.ecoamazonas.eco_agua.user;

import java.time.LocalTime;

public class EmployeeAttendanceRow {

    private Long attendanceId;
    private Long employeeId;
    private String employeeName;
    private String jobPositionName;
    private EmployeeAttendanceStatus status;
    private LocalTime startTime;
    private LocalTime endTime;
    private String observation;
    private boolean registered;

    public Long getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getJobPositionName() {
        return jobPositionName;
    }

    public void setJobPositionName(String jobPositionName) {
        this.jobPositionName = jobPositionName;
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

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public String getFormStatusName() {
        return status != null ? status.name() : EmployeeAttendanceStatus.PRESENT.name();
    }

    public String getStatusLabel() {
        if (!registered || status == null) {
            return "Sin registrar";
        }
        return switch (status) {
            case PRESENT -> "Presente";
            case LATE -> "Tardanza";
            case ABSENT -> "Falta";
            case PERMISSION -> "Permiso";
            case REST -> "Descanso";
        };
    }

    public String getStatusBadgeClass() {
        if (!registered || status == null) {
            return "bg-secondary";
        }
        return switch (status) {
            case PRESENT -> "bg-success";
            case LATE -> "bg-warning text-dark";
            case ABSENT -> "bg-danger";
            case PERMISSION -> "bg-info text-dark";
            case REST -> "bg-primary";
        };
    }
}
