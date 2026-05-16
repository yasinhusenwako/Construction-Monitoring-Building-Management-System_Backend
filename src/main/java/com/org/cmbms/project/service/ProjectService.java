
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final com.org.cmbms.project.repository.ProjectAssignmentRepository projectAssignmentRepository;
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
        project.setBlock(dto.getBlock());
        project.setFloor(dto.getFloor());
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
        project.setRequestMode(dto.getRequestMode());
        project.setLinkedProjectId(dto.getLinkedProjectId());
        project.setScope(dto.getScope());
        project.setStatus(Status.SUBMITTED);
        project.setCreatedBy(currentUser.getId());
        project.setCreatedAt(LocalDateTime.now());
        if (dto.getDivisionId() != null) {
            DivisionRules.assertAllowed(dto.getDivisionId());
        }
        project.setDivisionId(dto.getDivisionId());
        
        if (dto.getScope() != null) {
            project.setScope(dto.getScope());
        }
        
        Project saved = projectRepository.save(project);
        requestLifecycleService.initialize(RequestType.PROJECT, saved.getId(), currentUser.getId());
        
        // Notify all admins (both database and Keycloak) about new project submission
        requestLifecycleService.notifyUsersByRole(Role.ADMIN, "New Project Request", 
            "Project " + saved.getProjectId() + " has been submitted by " + currentUser.getEmail());
        
        return saved;
    }

    @Transactional
    public Project update(Long id, ProjectRequestDTO dto, UserPrincipal currentUser) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ApiException("Project not found"));
        
        // Only the creator can edit, and only if status is SUBMITTED
        if (!project.getCreatedBy().equals(currentUser.getId())) {
            throw new ApiException("You can only edit your own requests");
        }
        if (project.getStatus() != Status.SUBMITTED) {
            throw new ApiException("Can only edit requests in Submitted status");
        }
        
        // Update fields - keep existing values if new ones are null/empty
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            project.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            project.setDescription(dto.getDescription());
        }
        if (dto.getClassification() != null && !dto.getClassification().isBlank()) {
            project.setClassification(dto.getClassification());
        }
        if (dto.getLocation() != null && !dto.getLocation().isBlank()) {
            project.setLocation(dto.getLocation());
        }
        if (dto.getBudget() != null) {
            project.setBudget(dto.getBudget());
        }
        if (dto.getStartDate() != null) {
            project.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            project.setEndDate(dto.getEndDate());
        }
        if (dto.getDepartment() != null) {
            project.setDepartment(dto.getDepartment());
        }
        if (dto.getContactPerson() != null) {
            project.setContactPerson(dto.getContactPerson());
        }
        if (dto.getPhone() != null) {
            project.setPhone(dto.getPhone());
        }
        if (dto.getSiteCondition() != null) {
            project.setSiteCondition(dto.getSiteCondition());
        }
        if (dto.getBlock() != null) {
            project.setBlock(dto.getBlock());
        }
        if (dto.getFloor() != null) {
            project.setFloor(dto.getFloor());
        }
        // IMPORTANT: Update scope field which contains classification-specific data
        if (dto.getScope() != null) {
            project.setScope(dto.getScope());
        }
        
        // Add timeline event for the edit
        requestLifecycleService.recordNote(RequestType.PROJECT, project.getId(), currentUser.getId(), "Project details were updated");
        
        return projectRepository.save(project);
    }

    @Transactional
    public void delete(Long id, UserPrincipal currentUser) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ApiException("Project not found"));
        
        // Users can only delete their own requests in Submitted status
        // Admins can delete any request
        if (currentUser.getRole() == Role.USER) {
            if (!project.getCreatedBy().equals(currentUser.getId())) {
                throw new ApiException("You can only delete your own requests");
            }
            if (project.getStatus() != Status.SUBMITTED) {
                throw new ApiException("You can only delete requests in Submitted status");
            }
        } else if (currentUser.getRole() != Role.ADMIN) {
            throw new ApiException("Only users and admins can delete requests");
        }
        
        System.out.println("=== DELETING PROJECT ===");
        System.out.println("Project ID: " + project.getId());
        System.out.println("Project Business ID: " + project.getProjectId());
        System.out.println("Deleted by: " + currentUser.getUsername() + " (Role: " + currentUser.getRole() + ")");
        
        projectRepository.delete(project);
        
        System.out.println("=== PROJECT DELETED ===");
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
            // For Keycloak users, use email as ID
            String professionalId = currentUser.getId();
            System.out.println("=== PROFESSIONAL FETCHING PROJECTS ===");
            System.out.println("Professional ID: " + professionalId);
            System.out.println("Professional Email: " + currentUser.getEmail());
            System.out.println("Professional Role: " + currentUser.getRole());
            
            // MULTI-PROFESSIONAL UPDATE: Fetch assignments instead of a single string
            List<com.org.cmbms.project.model.ProjectAssignment> assignments = projectAssignmentRepository.findActiveAssignmentsByProfessionalId(professionalId);
            List<Long> projectIds = assignments.stream().map(com.org.cmbms.project.model.ProjectAssignment::getProjectId).collect(java.util.stream.Collectors.toList());
            
            // Also include legacy single-assigned projects just in case
            List<Project> legacyProjects = projectRepository.findByAssignedProfessionalId(professionalId);
            for (Project p : legacyProjects) {
                if (!projectIds.contains(p.getId())) {
                    projectIds.add(p.getId());
                }
            }
            
            if (projectIds.isEmpty()) return new ArrayList<>();
            
            List<Project> projects = projectRepository.findAllById(projectIds);
            System.out.println("Found " + projects.size() + " projects assigned to professional");
            for (Project p : projects) {
                System.out.println("  - Project: " + p.getProjectId());
            }
            
            return projects;
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
                                String divisionId,
                                String createdBy, // Changed to String
                                LocalDate startDate,
                                LocalDate endDate) {
        // USER role: return only projects they created
        if (currentUser.getRole() == Role.USER) {
            String userId = currentUser.getId();
            System.out.println("=== USER FETCHING PROJECTS ===");
            System.out.println("User ID: " + userId);
            System.out.println("User ID type: " + (userId != null ? userId.getClass().getName() : "null"));
            System.out.println("User ID length: " + (userId != null ? userId.length() : 0));
            
            List<Project> userProjects = projectRepository.findByCreatedBy(userId);
            System.out.println("Found " + userProjects.size() + " projects created by user");
            
            // Debug: Show all projects in database
            List<Project> allProjects = projectRepository.findAll();
            System.out.println("Total projects in database: " + allProjects.size());
            if (!allProjects.isEmpty()) {
                System.out.println("Sample project createdBy values:");
                for (int i = 0; i < Math.min(5, allProjects.size()); i++) {
                    Project p = allProjects.get(i);
                    System.out.println("  - Project " + p.getProjectId() + ": createdBy='" + p.getCreatedBy() + "' (length=" + (p.getCreatedBy() != null ? p.getCreatedBy().length() : 0) + ")");
                }
            }
            
            return userProjects;
        }
        
        if (currentUser.getRole() == Role.PROFESSIONAL) {
            // For professionals, only return projects assigned to them
            String professionalId = currentUser.getId();
            List<com.org.cmbms.project.model.ProjectAssignment> assignments = projectAssignmentRepository.findActiveAssignmentsByProfessionalId(professionalId);
            List<Long> projectIds = assignments.stream().map(com.org.cmbms.project.model.ProjectAssignment::getProjectId).collect(java.util.stream.Collectors.toList());
            
            // Add legacy single-assigned projects
            List<Project> legacyProjects = projectRepository.findByAssignedProfessionalId(professionalId);
            for (Project p : legacyProjects) {
                if (!projectIds.contains(p.getId())) {
                    projectIds.add(p.getId());
                }
            }
            
            if (projectIds.isEmpty()) return new ArrayList<>();
            return projectRepository.findAllById(projectIds);
        }
        
        if (currentUser.getRole() == Role.SUPERVISOR) {
            // Supervisors see projects assigned to them OR in their division
            String supervisorId = currentUser.getId();
            System.out.println("=== SUPERVISOR FETCHING PROJECTS ===");
            System.out.println("Supervisor ID: " + supervisorId);
            System.out.println("Supervisor Division: " + currentUser.getDivisionId());
            
            List<Project> assignedToMe = projectRepository.findByAssignedSupervisorId(supervisorId);
            List<Project> inMyDivision = projectRepository.findByDivisionId(currentUser.getDivisionId());
            
            // Combine and deduplicate
            Set<Project> combined = new HashSet<>(assignedToMe);
            combined.addAll(inMyDivision);
            
            System.out.println("Found " + assignedToMe.size() + " projects assigned to supervisor");
            System.out.println("Found " + inMyDivision.size() + " projects in supervisor's division");
            System.out.println("Total unique: " + combined.size());
            
            return new ArrayList<>(combined);
        }
        
        // Admin filtering based on workflow stages
        if (currentUser.getRole() == Role.ADMIN) {
            Specification<Project> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                // Admin sees items in admin-owned workflow stages
                // Projects don't have "Assigned to Supervisor" or "WorkOrder Created" stages
                // IMPORTANT: Include IN_PROGRESS and COMPLETED so admin can see projects being worked on
                List<Status> adminStages = List.of(
                    Status.SUBMITTED,
                    Status.UNDER_REVIEW,
                    Status.ASSIGNED_TO_PROFESSIONALS,
                    Status.IN_PROGRESS,
                    Status.COMPLETED,
                    Status.APPROVED,
                    Status.REJECTED,
                    Status.CLOSED
                );
                predicates.add(root.get("status").in(adminStages));
                
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
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            return projectRepository.findAll(spec);
        }
        
        // Default: return empty list for unknown roles
        return new ArrayList<>();
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

    public Project adminReject(Long id, String reason, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Project project = projectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));
        project.setStatus(Status.REJECTED);
        project.setRejectionReason(reason);
        requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.REJECTED, admin.getId());
        String notificationMessage = "Project " + project.getProjectId() + " rejected";
        if (reason != null && !reason.isBlank()) {
            notificationMessage += ". Reason: " + reason;
        }
        requestLifecycleService.notifyUser(project.getCreatedBy(), "Project rejected", notificationMessage);
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
    public Project adminAssignProfessional(Long id, String professionalId, String instructions, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Project project = projectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));

        System.out.println("=== ASSIGNING PROJECT TO PROFESSIONAL ===");
        System.out.println("Project ID: " + project.getId());
        System.out.println("Project Business ID: " + project.getProjectId());
        System.out.println("Professional ID (received): " + professionalId);
        System.out.println("Professional ID type: " + professionalId.getClass().getName());
        System.out.println("Current Status: " + project.getStatus());

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
        
        Project saved = projectRepository.save(project);
        
        // Notify the professional about the assignment
        requestLifecycleService.notifyUser(professionalId, "New Project Assignment", 
            "Project " + project.getProjectId() + " has been assigned to you");
        
        System.out.println("New Status: " + saved.getStatus());
        System.out.println("Assigned Professional ID (stored): " + saved.getAssignedProfessionalId());
        System.out.println("=== ASSIGNMENT COMPLETE ===");
        
        return saved;
    }

    public Project adminAssign(Long id, String divisionId, String supervisorId, String priority, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Project project = projectRepository.findById(id).orElseThrow(() -> new ApiException("Project not found"));
        if (divisionId == null) {
            throw new ApiException("Division is required");
        }
        DivisionRules.assertAllowed(divisionId);

        String finalSupervisorId;
        if (supervisorId != null) {
            // SupervisorId can be either numeric (database user) or email (Keycloak user)
            finalSupervisorId = supervisorId;
            
            // Try to validate if it's a database user
            try {
                Long numericId = Long.parseLong(supervisorId);
                User supervisor = userRepository.findById(numericId).orElseThrow(() -> new ApiException("Supervisor not found"));
                if (supervisor.getRole() != Role.SUPERVISOR) {
                    throw new ApiException("Selected user is not a supervisor");
                }
                if (supervisor.getDivisionId() == null || !supervisor.getDivisionId().equals(divisionId)) {
                    throw new ApiException("Supervisor must belong to selected division");
                }
            } catch (NumberFormatException e) {
                // It's a Keycloak user (email) - we'll trust the frontend validation
                System.out.println("Assigning to Keycloak supervisor: " + supervisorId);
            }
        } else {
            List<User> supervisors = userRepository.findByRoleAndDivisionId(Role.SUPERVISOR, divisionId);
            if (supervisors.isEmpty()) {
                throw new ApiException("No supervisor account found for selected division");
            }
            if (supervisors.size() > 1) {
                throw new ApiException("Multiple supervisor accounts found for selected division. Keep only one account per division.");
            }
            finalSupervisorId = String.valueOf(supervisors.get(0).getId());
        }

        project.setDivisionId(divisionId);
        project.setAssignedSupervisorId(finalSupervisorId);
        if (priority != null && !priority.isBlank()) {
            project.setPriority(priority);
        }
        requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.UNDER_REVIEW, admin.getId());
        requestLifecycleService.transition(RequestType.PROJECT, project.getId(), Status.ASSIGNED_TO_SUPERVISOR, admin.getId());
        project.setStatus(Status.ASSIGNED_TO_SUPERVISOR);
        requestLifecycleService.notifyUser(finalSupervisorId, "New assignment", "Project " + project.getProjectId() + " assigned to you");
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
        
        boolean isAssigned = projectAssignmentRepository.existsByProjectIdAndProfessionalId(project.getId(), professional.getId());
        if (!isAssigned && (project.getAssignedProfessionalId() == null || !project.getAssignedProfessionalId().equals(professional.getId()))) {
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
        
        boolean isAssigned = projectAssignmentRepository.existsByProjectIdAndProfessionalId(project.getId(), professional.getId());
        if (!isAssigned && (project.getAssignedProfessionalId() == null || !project.getAssignedProfessionalId().equals(professional.getId()))) {
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
        
        // Notify admins (both database and Keycloak) and supervisor about status change
        String notificationTitle = newStatus == Status.IN_PROGRESS ? "Project work started" : "Project completed";
        String notificationMessage = "Project " + project.getProjectId() + " is now " + newStatus.getValue();
        
        requestLifecycleService.notifyUsersByRole(Role.ADMIN, notificationTitle, notificationMessage);
        
        if (project.getAssignedSupervisorId() != null) {
            requestLifecycleService.notifyUser(project.getAssignedSupervisorId(), notificationTitle, notificationMessage);
        }
        
        return projectRepository.save(project);
    }

}
