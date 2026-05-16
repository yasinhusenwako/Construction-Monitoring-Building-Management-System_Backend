package com.org.cmbms.project.repository;

import com.org.cmbms.project.model.ProjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, Long> {
    
    // Get all assignments for a specific project
    List<ProjectAssignment> findByProjectId(Long projectId);
    
    // Get all active (non-inactive) assignments for a specific project
    @Query("SELECT pa FROM ProjectAssignment pa WHERE pa.projectId = :projectId AND pa.status <> 'INACTIVE'")
    List<ProjectAssignment> findActiveAssignmentsByProjectId(@Param("projectId") Long projectId);
    
    // Get all assignments for a specific professional
    List<ProjectAssignment> findByProfessionalId(String professionalId);
    
    // Get all active (non-inactive) assignments for a specific professional
    @Query("SELECT pa FROM ProjectAssignment pa WHERE pa.professionalId = :professionalId AND pa.status <> 'INACTIVE'")
    List<ProjectAssignment> findActiveAssignmentsByProfessionalId(@Param("professionalId") String professionalId);
    
    // Check if a professional is already assigned to a project
    boolean existsByProjectIdAndProfessionalId(Long projectId, String professionalId);
    
    // Get specific assignment
    Optional<ProjectAssignment> findByProjectIdAndProfessionalId(Long projectId, String professionalId);
}
