
package com.org.cmbms.project.repository;

import com.org.cmbms.project.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {
    List<Project> findByDivisionId(Long divisionId);
    Optional<Project> findByProjectId(String projectId);
}
