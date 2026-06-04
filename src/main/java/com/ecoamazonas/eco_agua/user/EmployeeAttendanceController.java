package com.ecoamazonas.eco_agua.user;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/personnel/attendance")
public class EmployeeAttendanceController {

    private final EmployeeAttendanceService attendanceService;

    public EmployeeAttendanceController(EmployeeAttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public String index(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        EmployeeAttendanceDailySnapshot snapshot = attendanceService.buildDailyAttendance(date);
        EmployeeAttendanceForm attendanceForm = buildForm(snapshot);

        model.addAttribute("activePage", "personnel_attendance");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("attendanceForm", attendanceForm);
        model.addAttribute("attendanceStatuses", EmployeeAttendanceStatus.values());

        return "admin/personnel_attendance";
    }

    @PostMapping("/save")
    public String save(
            @ModelAttribute("attendanceForm") EmployeeAttendanceForm attendanceForm,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate selectedDate = attendanceForm != null && attendanceForm.getAttendanceDate() != null
                ? attendanceForm.getAttendanceDate()
                : LocalDate.now();

        try {
            attendanceService.saveDailyAttendance(attendanceForm);
            redirectAttributes.addFlashAttribute("message", "Asistencia guardada correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al guardar asistencia: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/personnel/attendance?date=" + selectedDate;
    }

    private EmployeeAttendanceForm buildForm(EmployeeAttendanceDailySnapshot snapshot) {
        EmployeeAttendanceForm form = new EmployeeAttendanceForm();
        form.setAttendanceDate(snapshot.getSelectedDate());
        form.setItems(snapshot.getRows().stream()
                .map(this::buildItemForm)
                .toList());

        return form;
    }

    private EmployeeAttendanceItemForm buildItemForm(EmployeeAttendanceRow row) {
        EmployeeAttendanceItemForm item = new EmployeeAttendanceItemForm();
        item.setEmployeeId(row.getEmployeeId());
        item.setStatus(row.getStatus() != null ? row.getStatus() : EmployeeAttendanceStatus.PRESENT);
        item.setStartTime(row.getStartTime());
        item.setEndTime(row.getEndTime());
        item.setObservation(row.getObservation());

        return item;
    }
}
