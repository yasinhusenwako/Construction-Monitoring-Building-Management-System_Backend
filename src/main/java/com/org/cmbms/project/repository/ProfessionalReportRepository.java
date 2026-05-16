package com.org.cmbms.project.repository;

import com.org.cmbms.project.model.ProfessionalReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfessionalReportRepository extends JpaRepository<ProfessionalReport, Long> {
    
    // Get all reports for a specific assignment
    List<ProfessionalReport> findByAssignmentIdOrderByCreatedAtDesc(Long assignmentId);
    
    // Get reports for a specific project (through assignments)
    @Query("SELECT pr FROM ProfessionalReport pr " +
           "WHERE pr.assignmentId IN (SELECT pa.id FROM ProjectAssignment pa WHERE pa.projectId = :projectId) " +
           "ORDER BY pr.createdAt DESC")
    List<ProfessionalReport> findReportsByProjectId(@Param("projectId") Long projectId);
    
    // Get reports for a specific professional (through assignments)
    @Query("SELECT pr FROM ProfessionalReport pr " +
           "WHERE pr.assignmentId IN (SELECT pa.id FROM ProjectAssignment pa WHERE pa.professionalId = :professionalId) " +
           "ORDER BY pr.createdAt DESC")
    List<ProfessionalReport> findReportsByProfessionalId(@Param("professionalId") String professionalId);
}
