package com.ecoamazonas.eco_agua.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EmployeeAttendanceService {

    private static final int MAX_OBSERVATION_LENGTH = 500;
    private static final Locale SPANISH_LOCALE = new Locale("es", "PE");
    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmployeeRepository employeeRepository;
    private final EmployeeAttendanceRepository attendanceRepository;

    public EmployeeAttendanceService(
            EmployeeRepository employeeRepository,
            EmployeeAttendanceRepository attendanceRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Transactional(readOnly = true)
    public EmployeeAttendanceDailySnapshot buildDailyAttendance(LocalDate date) {
        LocalDate selectedDate = resolveDate(date);
        List<Employee> employees = employeeRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc();
        Map<Long, EmployeeAttendance> attendanceByEmployeeId = attendanceRepository.findByAttendanceDateWithEmployee(selectedDate).stream()
                .filter(attendance -> attendance.getEmployee() != null && attendance.getEmployee().getId() != null)
                .collect(Collectors.toMap(
                        attendance -> attendance.getEmployee().getId(),
                        Function.identity(),
                        (first, second) -> first
                ));

        List<EmployeeAttendanceRow> rows = employees.stream()
                .map(employee -> buildRow(employee, attendanceByEmployeeId.get(employee.getId())))
                .sorted(Comparator.comparing(EmployeeAttendanceRow::getEmployeeName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        EmployeeAttendanceDailySnapshot snapshot = new EmployeeAttendanceDailySnapshot();
        snapshot.setSelectedDate(selectedDate);
        snapshot.setSelectedDateLabel(buildDateLabel(selectedDate));
        snapshot.setRows(rows);
        snapshot.setSummary(buildSummary(rows));

        return snapshot;
    }

    @Transactional
    public void saveDailyAttendance(EmployeeAttendanceForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Attendance form is required.");
        }

        LocalDate attendanceDate = resolveDate(form.getAttendanceDate());
        List<EmployeeAttendanceItemForm> items = form.getItems();
        if (items == null || items.isEmpty()) {
            return;
        }

        for (EmployeeAttendanceItemForm item : items) {
            if (item == null || item.getEmployeeId() == null || item.getEmployeeId() <= 0) {
                continue;
            }

            Employee employee = employeeRepository.findById(item.getEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Employee was not found: " + item.getEmployeeId()));

            EmployeeAttendance attendance = attendanceRepository
                    .findByEmployeeIdAndAttendanceDate(employee.getId(), attendanceDate)
                    .orElseGet(EmployeeAttendance::new);

            attendance.setEmployee(employee);
            attendance.setAttendanceDate(attendanceDate);
            attendance.setStatus(resolveStatus(item.getStatus()));
            attendance.setStartTime(resolveTime(item.getStartTime()));
            attendance.setEndTime(resolveTime(item.getEndTime()));
            attendance.setObservation(cleanObservation(item.getObservation()));

            attendanceRepository.save(attendance);
        }
    }

    public String buildStatusLabel(EmployeeAttendanceStatus status) {
        if (status == null) {
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

    private EmployeeAttendanceRow buildRow(Employee employee, EmployeeAttendance attendance) {
        EmployeeAttendanceRow row = new EmployeeAttendanceRow();
        row.setEmployeeId(employee.getId());
        row.setEmployeeName(buildEmployeeName(employee));
        row.setJobPositionName(buildJobPositionName(employee));

        if (attendance != null) {
            row.setAttendanceId(attendance.getId());
            row.setStatus(attendance.getStatus());
            row.setStartTime(attendance.getStartTime());
            row.setEndTime(attendance.getEndTime());
            row.setObservation(cleanText(attendance.getObservation()));
            row.setRegistered(true);
        } else {
            row.setStatus(null);
            row.setObservation("");
            row.setRegistered(false);
        }

        return row;
    }

    private EmployeeAttendanceSummary buildSummary(List<EmployeeAttendanceRow> rows) {
        EmployeeAttendanceSummary summary = new EmployeeAttendanceSummary();
        summary.setEmployeeCount(rows.size());
        summary.setRegisteredCount((int) rows.stream().filter(EmployeeAttendanceRow::isRegistered).count());
        summary.setPendingCount((int) rows.stream().filter(row -> !row.isRegistered()).count());
        summary.setPresentCount(countStatus(rows, EmployeeAttendanceStatus.PRESENT));
        summary.setLateCount(countStatus(rows, EmployeeAttendanceStatus.LATE));
        summary.setAbsentCount(countStatus(rows, EmployeeAttendanceStatus.ABSENT));
        summary.setPermissionCount(countStatus(rows, EmployeeAttendanceStatus.PERMISSION));
        summary.setRestCount(countStatus(rows, EmployeeAttendanceStatus.REST));

        return summary;
    }

    private int countStatus(List<EmployeeAttendanceRow> rows, EmployeeAttendanceStatus status) {
        return (int) rows.stream()
                .filter(EmployeeAttendanceRow::isRegistered)
                .map(EmployeeAttendanceRow::getStatus)
                .filter(item -> item == status)
                .count();
    }

    private EmployeeAttendanceStatus resolveStatus(EmployeeAttendanceStatus status) {
        return status != null ? status : EmployeeAttendanceStatus.PRESENT;
    }

    private LocalDate resolveDate(LocalDate date) {
        return date != null ? date : LocalDate.now();
    }

    private LocalTime resolveTime(LocalTime time) {
        return time;
    }

    private String buildEmployeeName(Employee employee) {
        if (employee == null) {
            return "Sin trabajador";
        }

        String firstName = cleanText(employee.getFirstName());
        String lastName = cleanText(employee.getLastName());
        String fullName = (firstName + " " + lastName).trim();

        return fullName.isBlank() ? "Trabajador sin nombre" : fullName;
    }

    private String buildJobPositionName(Employee employee) {
        if (employee == null || employee.getJobPosition() == null) {
            return "Sin cargo asignado";
        }

        String name = cleanText(employee.getJobPosition().getName());
        return name.isBlank() ? "Sin cargo asignado" : name;
    }

    private String buildDateLabel(LocalDate date) {
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, SPANISH_LOCALE);
        return capitalize(dayName) + ", " + date.format(DATE_LABEL_FORMATTER);
    }

    private String capitalize(String value) {
        String cleanValue = cleanText(value);
        if (cleanValue.isBlank()) {
            return cleanValue;
        }

        return cleanValue.substring(0, 1).toUpperCase(SPANISH_LOCALE) + cleanValue.substring(1);
    }

    private String cleanObservation(String value) {
        String cleanValue = cleanText(value);
        if (cleanValue.length() <= MAX_OBSERVATION_LENGTH) {
            return cleanValue;
        }

        return cleanValue.substring(0, MAX_OBSERVATION_LENGTH);
    }

    private String cleanText(String value) {
        return Objects.toString(value, "").trim();
    }
}
