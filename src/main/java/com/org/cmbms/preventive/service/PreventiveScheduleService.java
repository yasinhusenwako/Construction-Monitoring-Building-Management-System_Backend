package com.org.cmbms.preventive.service;

import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.preventive.dto.PreventiveScheduleRequest;
import com.org.cmbms.preventive.model.PreventiveSchedule;
import com.org.cmbms.preventive.repository.PreventiveScheduleRepository;
import com.org.cmbms.user.model.User;
import com.org.cmbms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PreventiveScheduleService {

    private final PreventiveScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    public List<PreventiveSchedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    public PreventiveSchedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ApiException("Schedule not found"));
    }

    public PreventiveSchedule getScheduleByScheduleId(String scheduleId) {
        return scheduleRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new ApiException("Schedule not found"));
    }

    @Transactional
    public PreventiveSchedule createSchedule(PreventiveScheduleRequest request) {
        // Validate assigned professional exists
        User professional = userRepository.findById(request.getAssignedProfessionalId())
                .orElseThrow(() -> new ApiException("Professional not found"));

        // Generate schedule ID
        String scheduleId = generateScheduleId();

        PreventiveSchedule schedule = new PreventiveSchedule();
        schedule.setScheduleId(scheduleId);
        schedule.setSystem(request.getSystem());
        schedule.setFrequency(request.getFrequency());
        schedule.setLastDone(request.getLastDone());
        schedule.setNextDue(request.getNextDue());
        schedule.setAssignedProfessionalId(request.getAssignedProfessionalId());
        schedule.setAssignee(professional.getName());
        schedule.setNotes(request.getNotes());
        schedule.setStatus("Scheduled"); // Will be auto-updated in @PrePersist

        return scheduleRepository.save(schedule);
    }

    @Transactional
    public PreventiveSchedule updateSchedule(Long id, PreventiveScheduleRequest request) {
        PreventiveSchedule schedule = getScheduleById(id);

        if (request.getSystem() != null) {
            schedule.setSystem(request.getSystem());
        }
        if (request.getFrequency() != null) {
            schedule.setFrequency(request.getFrequency());
        }
        if (request.getLastDone() != null) {
            schedule.setLastDone(request.getLastDone());
        }
        if (request.getNextDue() != null) {
            schedule.setNextDue(request.getNextDue());
        }
        if (request.getAssignedProfessionalId() != null) {
            User professional = userRepository.findById(request.getAssignedProfessionalId())
                    .orElseThrow(() -> new ApiException("Professional not found"));
            schedule.setAssignedProfessionalId(request.getAssignedProfessionalId());
            schedule.setAssignee(professional.getName());
        }
        if (request.getNotes() != null) {
            schedule.setNotes(request.getNotes());
        }

        return scheduleRepository.save(schedule);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        PreventiveSchedule schedule = getScheduleById(id);
        scheduleRepository.delete(schedule);
    }

    @Transactional
    public PreventiveSchedule markAsCompleted(Long id) {
        PreventiveSchedule schedule = getScheduleById(id);
        
        // Update last done to today
        schedule.setLastDone(LocalDate.now());
        
        // Calculate next due date based on frequency
        LocalDate nextDue = calculateNextDueDate(LocalDate.now(), schedule.getFrequency());
        schedule.setNextDue(nextDue);
        
        return scheduleRepository.save(schedule);
    }

    private String generateScheduleId() {
        long count = scheduleRepository.count();
        return String.format("PM-%03d", count + 1);
    }

    private LocalDate calculateNextDueDate(LocalDate from, String frequency) {
        // Parse frequency like "Every 3 months", "Every 6 months", "Every month"
        String[] parts = frequency.toLowerCase().split(" ");
        
        try {
            if (frequency.toLowerCase().contains("month")) {
                int months = 1;
                if (parts.length > 1 && !parts[1].equals("month")) {
                    months = Integer.parseInt(parts[1]);
                }
                return from.plusMonths(months);
            } else if (frequency.toLowerCase().contains("week")) {
                int weeks = 1;
                if (parts.length > 1 && !parts[1].equals("week")) {
                    weeks = Integer.parseInt(parts[1]);
                }
                return from.plusWeeks(weeks);
            } else if (frequency.toLowerCase().contains("year")) {
                int years = 1;
                if (parts.length > 1 && !parts[1].equals("year")) {
                    years = Integer.parseInt(parts[1]);
                }
                return from.plusYears(years);
            }
        } catch (NumberFormatException e) {
            // Default to 3 months if parsing fails
            return from.plusMonths(3);
        }
        
        // Default to 3 months
        return from.plusMonths(3);
    }

    public List<PreventiveSchedule> getOverdueSchedules() {
        return scheduleRepository.findByStatus("Overdue");
    }

    public List<PreventiveSchedule> getDueSoonSchedules() {
        LocalDate today = LocalDate.now();
        LocalDate weekFromNow = today.plusDays(7);
        return scheduleRepository.findByNextDueBetween(today, weekFromNow);
    }
}
