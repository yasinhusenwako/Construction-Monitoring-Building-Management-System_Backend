
package com.org.cmbms.project.service;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.enums.RequestType;
import com.org.cmbms.common.enums.Status;
import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.file.model.FileRecord;
import com.org.cmbms.file.service.FileStorageService;
import com.org.cmbms.project.dto.BoqResponse;
import com.org.cmbms.project.dto.ProjectRequestDTO;
import com.org.cmbms.project.model.Project;
import com.org.cmbms.project.repository.ProjectRepository;
import com.org.cmbms.user.model.User;
import com.org.cmbms.user.repository.UserRepository;
import com.org.cmbms.workflow.service.RequestLifecycleService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.jpa.domain.Specification;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorageService;
    private final RequestLifecycleService requestLifecycleService;
    private final UserRepository userRepository;

    public Project create(ProjectRequestDTO dto, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.PROFESSIONAL) {
            throw new ApiException("Professional cannot access projects");
        }
        Project project = new Project();
        project.setProjectId(dto.getProjectId());
        project.setTitle(dto.getTitle());
        project.setLocation(dto.getLocation());
        project.setDepartment(dto.getDepartment());
        project.setContactPerson(dto.getContactPerson());
        project.setPhone(dto.getPhone());
        project.setSiteCondition(dto.getSiteCondition());
        project.setDescription(dto.getDescription());
        project.setBudget(dto.getBudget());
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        project.setClassification(dto.getClassification());
        project.setPriority(dto.getPriority());
        project.setStatus(Status.SUBMITTED);
        project.setCreatedBy(currentUser.getId());
        project.setCreatedAt(LocalDateTime.now());
        project.setDivisionId(dto.getDivisionId());
        Project saved = projectRepository.save(project);
        requestLifecycleService.initialize(RequestType.PROJECT, saved.getId(), currentUser.getId());
        return saved;
    }

    public BoqResponse submitBoq(String projectId, UserPrincipal currentUser) {
        Project project = projectRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ApiException("Project not found"));
        if (project.getStatus() != Status.APPROVED) {
            throw new ApiException("BOQ requires approved projectId");
        }
        if (currentUser.getRole() == Role.PROFESSIONAL) {
            throw new ApiException("Access denied");
        }
        project.setBoqApproved(Boolean.TRUE);
        projectRepository.save(project);
        return new BoqResponse(projectId, "BOQ accepted");
    }

    public List<Project> all() {
        return projectRepository.findAll();
    }

    public List<Project> all(UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.USER) {
            return projectRepository.findAll();
        }
        if (currentUser.getRole() == Role.PROFESSIONAL) {
            return projectRepository.findByAssignedProfessionalId(currentUser.getId());
        }
        if (currentUser.getDivisionId() == null) {
            throw new ApiException("Division not set for user");
        }
        return projectRepository.findByDivisionId(currentUser.getDivisionId());
    }

    public List<Project> search(UserPrincipal currentUser,
                                String status,
                                String priority,
                                String projectId,
                                Long divisionId,
                                Long createdBy,
                                LocalDate startDate,
                                LocalDate endDate) {
        if (currentUser.getRole() == Role.PROFESSIONAL) {
            // For professionals, only return projects assigned to them
            return projectRepository.findByAssignedProfessionalId(currentUser.getId());
        }
        Specification<Project> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), Status.fromValue(status)));
            }
            if (priority != null && !priority.isBlank()) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (projectId != null && !projectId.isBlank()) {
                predicates.add(cb.equal(root.get("projectId"), projectId));
            }
            if (divisionId != null) {
                predicates.add(cb.equal(root.get("divisionId"), divisionId));
            }
            if (createdBy != null) {
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), endDate));
            }
            if (currentUser.getRole() == Role.SUPERVISOR) {
                predicates.add(cb.equal(root.get("divisionId"), currentUser.getDivisionId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return projectRepository.findAll(spec);
    }

    public FileRecord uploadDoc(Long projectId, MultipartFile file, Long userId, Role role) throws IOException {
        if (role == Role.PROFESSIONAL) {
            throw new ApiException("Professional cannot access projects");
        }
        return fileStorageService.save(projectId, "PROJECT_DOC", file, userId);
    }

    public Project supervisorReview(Long id, UserPrincipal supervisor) {
        if (supervisor.getRole() != Role.SUPERVISOR) {
            throw new ApiException("Access denied");
        }
        Project project = projectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));
        if (project.getDivisionId() == null || !project.getDivisionId().equals(supervisor.getDivisionId())) {
            throw new ApiException("supervisor sees only division requests");
        }
        project.setStatus(Status.REVIEWED);
        requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.REVIEWED, supervisor.getId());
        requestLifecycleService.notifyUser(project.getCreatedBy(), "Project reviewed", "Project " + project.getProjectId() + " reviewed");
        return projectRepository.save(project);
    }

    public Project adminStartReview(Long id, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Project project = projectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));
        project.setStatus(Status.UNDER_REVIEW);
        requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.UNDER_REVIEW, admin.getId());
        // notify assigned supervisor if present
        if (project.getAssignedSupervisorId() != null) {
            requestLifecycleService.notifyUser(project.getAssignedSupervisorId(), "Project under review", "Project " + project.getProjectId() + " is under review");
        }
        return projectRepository.save(project);
    }

    public Project adminApprove(Long id, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Project project = projectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));
        project.setStatus(Status.APPROVED);
        requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.APPROVED, admin.getId());
        requestLifecycleService.notifyUser(project.getCreatedBy(), "Project approved", "Project " + project.getProjectId() + " approved");
        return projectRepository.save(project);
    }

    public Project adminReject(Long id, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Project project = projectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));
        project.setStatus(Status.REJECTED);
        requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.REJECTED, admin.getId());
        requestLifecycleService.notifyUser(project.getCreatedBy(), "Project rejected", "Project " + project.getProjectId() + " rejected");
        return projectRepository.save(project);
    }

    public Project adminClose(Long id, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Project project = projectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));
        project.setStatus(Status.CLOSED);
        requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.CLOSED, admin.getId());
        requestLifecycleService.notifyUser(project.getCreatedBy(), "Project closed", "Project " + project.getProjectId() + " closed");
        return projectRepository.save(project);
    }

    
    public Project adminAssignProfessional(Long id, Long professionalId, String instructions, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Project project = projectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));
        User professional = userRepository.findById(professionalId).orElseThrow(() -> new ApiException("Professional not found"));
        if (professional.getRole() != Role.PROFESSIONAL) {
            throw new ApiException("Selected user is not a professional");
        }
        
        project.setAssignedProfessionalId(professionalId);
        project.setStatus(Status.ASSIGNED_TO_PROFESSIONALS);
        requestLifecycleService.transition(com.org.cmbms.common.enums.RequestType.PROJECT, project.getId(), Status.ASSIGNED_TO_PROFESSIONALS, admin.getId());
        requestLifecycleService.notifyUser(professionalId, "New assignment", "Project " + project.getProjectId() + " assigned to you");
        return projectRepository.save(project);
    }

    public Project adminAssign(Long id, Long divisionId, Long supervisorId, String priority, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Project project = projectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));
        User supervisor = userRepository.findById(supervisorId).orElseThrow(() -> new ApiException("Supervisor not found"));
        if (supervisor.getRole() != Role.SUPERVISOR) {
            throw new ApiException("Selected user is not a supervisor");
        }
        project.setDivisionId(divisionId);
        project.setAssignedSupervisorId(supervisorId);
        if (priority != null && !priority.isBlank()) {
            project.setPriority(priority);
        }
        requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.UNDER_REVIEW, admin.getId());
        requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.ASSIGNED_TO_SUPERVISOR, admin.getId());
        project.setStatus(Status.ASSIGNED_TO_SUPERVISOR);
        requestLifecycleService.notifyUser(supervisorId, "New assignment", "Project " + project.getProjectId() + " assigned to you");
        return projectRepository.save(project);
    }

}
