
package com.org.cmbms.project.service;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.enums.RequestType;
import com.org.cmbms.common.enums.Status;
import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.common.util.DivisionRules;
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
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.jpa.domain.Specification;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorageService;
    private final RequestLifecycleService requestLifecycleService;
    private final UserRepository userRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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
        if (dto.getDivisionId() != null) {
            DivisionRules.assertAllowed(dto.getDivisionId());
        }
        project.setDivisionId(dto.getDivisionId());
        
        if (dto.getScope() != null) {
            try {
                project.setScope(objectMapper.writeValueAsString(dto.getScope()));
            } catch (Exception e) {
                // ignore or log
            }
        }
        
        Project saved = projectRepository.save(project);
        requestLifecycleService.initialize(RequestType.PROJECT, saved.getId(), currentUser.getId());
        return saved;
    }

    public Project update(Long id, ProjectRequestDTO dto, UserPrincipal currentUser) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ApiException("Project not found"));
        
        if (currentUser.getRole() == Role.PROFESSIONAL) {
            throw new ApiException("Professional cannot update projects");
        }
        
        // Only allow update if in SUBMITTED or UNDER_REVIEW status and it's the owner (or admin)
        if (project.getStatus() != Status.SUBMITTED && project.getStatus() != Status.UNDER_REVIEW && currentUser.getRole() != Role.ADMIN) {
            throw new ApiException("Project cannot be edited in its current status");
        }
        
        if (currentUser.getRole() != Role.ADMIN && !project.getCreatedBy().equals(currentUser.getId())) {
            throw new ApiException("You are not authorized to edit this project");
        }

        if (dto.getTitle() != null) project.setTitle(dto.getTitle());
        if (dto.getLocation() != null) project.setLocation(dto.getLocation());
        if (dto.getDepartment() != null) project.setDepartment(dto.getDepartment());
        if (dto.getContactPerson() != null) project.setContactPerson(dto.getContactPerson());
        if (dto.getPhone() != null) project.setPhone(dto.getPhone());
        if (dto.getSiteCondition() != null) project.setSiteCondition(dto.getSiteCondition());
        if (dto.getDescription() != null) project.setDescription(dto.getDescription());
        if (dto.getBudget() != null) project.setBudget(dto.getBudget());
        if (dto.getStartDate() != null) project.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) project.setEndDate(dto.getEndDate());
        if (dto.getClassification() != null) project.setClassification(dto.getClassification());
        
        if (dto.getPriority() != null) {
            project.setPriority(dto.getPriority());
        }
        
        if (dto.getDivisionId() != null) {
            DivisionRules.assertAllowed(dto.getDivisionId());
            project.setDivisionId(dto.getDivisionId());
        }
        
        if (dto.getScope() != null) {
            try {
                project.setScope(objectMapper.writeValueAsString(dto.getScope()));
            } catch (Exception e) {
                // ignore
            }
        }

        return projectRepository.save(project);
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

    @Transactional
    public Project adminAssignProfessional(Long id, Long professionalId, String instructions, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Project project = projectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));
        User professional = userRepository.findById(professionalId).orElseThrow(() -> new ApiException("Professional not found"));
        if (professional.getRole() != Role.PROFESSIONAL) {
            throw new ApiException("Selected user is not a professional");
        }

        // For projects, admin can directly assign professional from Under Review
        Status current = project.getStatus();
        if (current == Status.SUBMITTED) {
            requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.UNDER_REVIEW, admin.getId());
            project.setStatus(Status.UNDER_REVIEW);
            current = Status.UNDER_REVIEW;
        }
        if (current != Status.UNDER_REVIEW && current != Status.ASSIGNED_TO_PROFESSIONALS) {
            throw new ApiException("Project must be under review before assigning a professional");
        }
        
        project.setAssignedProfessionalId(professionalId);
        if (current == Status.UNDER_REVIEW) {
            requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.ASSIGNED_TO_PROFESSIONALS, admin.getId());
        }
        project.setStatus(Status.ASSIGNED_TO_PROFESSIONALS);
        requestLifecycleService.notifyUser(professionalId, "New assignment", "Project " + project.getProjectId() + " assigned to you");
        return projectRepository.save(project);
    }

    public Project adminAssign(Long id, Long divisionId, Long supervisorId, String priority, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Project project = projectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));
        if (divisionId == null) {
            throw new ApiException("Division is required");
        }
        DivisionRules.assertAllowed(divisionId);

        User supervisor;
        if (supervisorId != null) {
            supervisor = userRepository.findById(supervisorId).orElseThrow(() -> new ApiException("Supervisor not found"));
            if (supervisor.getRole() != Role.SUPERVISOR) {
                throw new ApiException("Selected user is not a supervisor");
            }
            if (supervisor.getDivisionId() == null || !supervisor.getDivisionId().equals(divisionId)) {
                throw new ApiException("Supervisor must belong to selected division");
            }
        } else {
            List<User> supervisors = userRepository.findByRoleAndDivisionId(Role.SUPERVISOR, divisionId);
            if (supervisors.isEmpty()) {
                throw new ApiException("No supervisor account found for selected division");
            }
            if (supervisors.size() > 1) {
                throw new ApiException("Multiple supervisor accounts found for selected division. Keep only one account per division.");
            }
            supervisor = supervisors.get(0);
        }

        project.setDivisionId(divisionId);
        project.setAssignedSupervisorId(supervisor.getId());
        if (priority != null && !priority.isBlank()) {
            project.setPriority(priority);
        }
        requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.UNDER_REVIEW, admin.getId());
        requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.ASSIGNED_TO_SUPERVISOR, admin.getId());
        project.setStatus(Status.ASSIGNED_TO_SUPERVISOR);
        requestLifecycleService.notifyUser(supervisor.getId(), "New assignment", "Project " + project.getProjectId() + " assigned to you");
        return projectRepository.save(project);
    }
    
    @Transactional
    public Project updateProjectCost(Long id, BigDecimal materialCost, BigDecimal laborCost, String partsUsed, UserPrincipal professional) {
        System.out.println("=== updateProjectCost called ===");
        System.out.println("Project ID: " + id);
        System.out.println("Material Cost: " + materialCost);
        System.out.println("Labor Cost: " + laborCost);
        System.out.println("Parts Used: " + partsUsed);
        System.out.println("Professional ID: " + professional.getId());
        
        if (professional.getRole() != Role.PROFESSIONAL) {
            throw new ApiException("Access denied");
        }
        Project project = projectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));
        
        System.out.println("Found project: " + project.getProjectId());
        System.out.println("Assigned professional: " + project.getAssignedProfessionalId());
        
        if (!project.getAssignedProfessionalId().equals(professional.getId())) {
            throw new ApiException("You are not assigned to this project");
        }
        
        project.setMaterialCost(materialCost);
        project.setLaborCost(laborCost);
        project.setPartsUsed(partsUsed);
        
        BigDecimal total = BigDecimal.ZERO;
        if (materialCost != null) total = total.add(materialCost);
        if (laborCost != null) total = total.add(laborCost);
        project.setTotalCost(total);
        
        System.out.println("Saving project with total cost: " + total);
        Project saved = projectRepository.save(project);
        System.out.println("Project saved successfully. Material cost in DB: " + saved.getMaterialCost());
        
        return saved;
    }
    @Transactional
    public Project professionalUpdateStatus(Long id, String statusStr, UserPrincipal professional) {
        if (professional.getRole() != Role.PROFESSIONAL) {
            throw new ApiException("Access denied");
        }
        Project project = projectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));
        if (!project.getAssignedProfessionalId().equals(professional.getId())) {
            throw new ApiException("You are not assigned to this project");
        }
        Status current = project.getStatus();
        Status newStatus;
        if ("In Progress".equals(statusStr)) {
            newStatus = Status.IN_PROGRESS;
        } else if ("Completed".equals(statusStr)) {
            newStatus = Status.COMPLETED;
        } else {
            throw new ApiException("Invalid status");
        }
        
        if (current == Status.UNDER_REVIEW) {
            requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.ASSIGNED_TO_PROFESSIONALS, professional.getId());
            project.setStatus(Status.ASSIGNED_TO_PROFESSIONALS);
            current = Status.ASSIGNED_TO_PROFESSIONALS;
        }
        
        requestLifecycleService.transition(RequestType.PROJECT, project.getId(), newStatus, professional.getId());
        project.setStatus(newStatus);
        return projectRepository.save(project);
    }

}
