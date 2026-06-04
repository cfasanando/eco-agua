package com.ecoamazonas.eco_agua.user;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeAttendanceForm {

    private LocalDate attendanceDate;
    private List<EmployeeAttendanceItemForm> items = new ArrayList<>();

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public List<EmployeeAttendanceItemForm> getItems() {
        return items;
    }

    public void setItems(List<EmployeeAttendanceItemForm> items) {
        this.items = items;
    }
}
