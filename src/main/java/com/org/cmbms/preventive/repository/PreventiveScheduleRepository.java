package com.org.cmbms.preventive.repository;

import com.org.cmbms.preventive.model.PreventiveSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PreventiveScheduleRepository extends JpaRepository<PreventiveSchedule, Long> {
    
    Optional<PreventiveSchedule> findByScheduleId(String scheduleId);
    
    List<PreventiveSchedule> findByStatus(String status);
    
    List<PreventiveSchedule> findByAssignedProfessionalId(Long professionalId);
    
    List<PreventiveSchedule> findByNextDueBefore(LocalDate date);
    
    List<PreventiveSchedule> findByNextDueBetween(LocalDate start, LocalDate end);
}
