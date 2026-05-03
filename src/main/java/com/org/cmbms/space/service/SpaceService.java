
package com.org.cmbms.space.service;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.enums.RequestType;
import com.org.cmbms.common.enums.Status;
import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.common.util.DivisionRules;
import com.org.cmbms.space.dto.BookingRequestDTO;
import com.org.cmbms.space.model.Booking;
import com.org.cmbms.space.repository.SpaceRepository;
import com.org.cmbms.user.model.User;
import com.org.cmbms.user.repository.UserRepository;
import com.org.cmbms.workflow.service.RequestLifecycleService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final RequestLifecycleService requestLifecycleService;
    private final UserRepository userRepository;

    public Booking create(BookingRequestDTO dto, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.PROFESSIONAL) {
            throw new ApiException("Professional cannot access bookings");
        }
        if ("HALL".equalsIgnoreCase(dto.getType())) {
            LocalDateTime start = dto.getDateTime().minusHours(2);
            LocalDateTime end = dto.getDateTime().plusHours(2);
            String hallName = dto.getLayout() == null ? "DEFAULT_HALL" : dto.getLayout();
            if (!spaceRepository.findByTypeAndLayoutAndDateTimeBetween("HALL", hallName, start, end).isEmpty()) {
                throw new ApiException("Hall conflict detected for selected dateTime and hall");
            }
        }
        if ("OFFICE".equalsIgnoreCase(dto.getType())) {
            int availableInventory = 100; // Increased from 25 to 100
            if (dto.getCapacity() != null && dto.getCapacity() > 100) {
                throw new ApiException("Office inventory insufficient for requested capacity (max: 100)");
            }
            if (dto.getCapacity() != null && dto.getCapacity() > availableInventory) {
                throw new ApiException("Office inventory insufficient for requested capacity");
            }
        }
        Booking booking = new Booking();
        booking.setBookingId(dto.getBookingId());
        booking.setType(dto.getType());
        booking.setStatus(Status.SUBMITTED);
        // Use current user's email from Keycloak instead of DTO requester
        booking.setRequester(currentUser.getEmail());
        booking.setDateTime(dto.getDateTime());
        booking.setEndTime(dto.getEndTime());
        booking.setCapacity(dto.getCapacity());
        booking.setLayout(dto.getLayout());
        booking.setAmenities(dto.getAmenities());
        String selectedDivisionId;
        if (currentUser.getRole() == Role.SUPERVISOR && currentUser.getDivisionId() != null) {
            selectedDivisionId = currentUser.getDivisionId();
        } else {
            selectedDivisionId = dto.getDivisionId();
        }
        if (selectedDivisionId != null) {
            DivisionRules.assertAllowed(selectedDivisionId);
        }
        booking.setDivisionId(selectedDivisionId);
        Booking saved = spaceRepository.save(booking);
        requestLifecycleService.initialize(RequestType.BOOKING, saved.getId(), currentUser.getId());
        
        // Notify all admins (both database and Keycloak) about new booking submission
        requestLifecycleService.notifyUsersByRole(Role.ADMIN, "New Booking Request", 
            "Booking " + saved.getBookingId() + " has been submitted by " + currentUser.getEmail());
        
        return saved;
    }

    public Booking update(Long id, BookingRequestDTO dto, UserPrincipal currentUser) {
        Booking booking = spaceRepository.findById(id)
                .orElseThrow(() -> new ApiException("Booking not found"));
        
        if (currentUser.getRole() == Role.PROFESSIONAL) {
            throw new ApiException("Professional cannot update bookings");
        }
        
        if (booking.getStatus() != Status.SUBMITTED && booking.getStatus() != Status.UNDER_REVIEW && currentUser.getRole() != Role.ADMIN) {
            throw new ApiException("Booking cannot be edited in its current status");
        }
        
        // Ownership check
        if (currentUser.getRole() != Role.ADMIN && !booking.getRequester().equals(currentUser.getId())) {
            throw new ApiException("You are not authorized to edit this booking");
        }

        if (dto.getDateTime() != null && "HALL".equalsIgnoreCase(dto.getType() != null ? dto.getType() : booking.getType())) {
            LocalDateTime start = dto.getDateTime().minusHours(2);
            LocalDateTime end = dto.getDateTime().plusHours(2);
            String hallName = dto.getLayout() == null ? (booking.getLayout() == null ? "DEFAULT_HALL" : booking.getLayout()) : dto.getLayout();
            List<Booking> conflicts = spaceRepository.findByTypeAndLayoutAndDateTimeBetween("HALL", hallName, start, end);
            if (conflicts.stream().anyMatch(c -> !c.getId().equals(id))) {
                throw new ApiException("Hall conflict detected for selected dateTime and hall");
            }
        }

        if (dto.getType() != null) booking.setType(dto.getType());
        if (dto.getDateTime() != null) booking.setDateTime(dto.getDateTime());
        if (dto.getEndTime() != null) booking.setEndTime(dto.getEndTime());
        if (dto.getCapacity() != null) booking.setCapacity(dto.getCapacity());
        if (dto.getLayout() != null) booking.setLayout(dto.getLayout());
        if (dto.getAmenities() != null) booking.setAmenities(dto.getAmenities());
        
        if (dto.getDivisionId() != null) {
            DivisionRules.assertAllowed(dto.getDivisionId());
            booking.setDivisionId(dto.getDivisionId());
        }

        return spaceRepository.save(booking);
    }

    public List<Booking> all(UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.PROFESSIONAL) {
            // Find by assigned professional OR requester
            Specification<Booking> spec = (root, query, cb) -> cb.or(
                    cb.equal(root.get("assignedProfessionalId"), currentUser.getId()),
                    cb.equal(root.get("requester"), currentUser.getId())
            );
            return spaceRepository.findAll(spec);
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return spaceRepository.findAll();
        }
        if (currentUser.getRole() == Role.SUPERVISOR && currentUser.getDivisionId() != null) {
            return spaceRepository.findByDivisionId(currentUser.getDivisionId());
        }
        return spaceRepository.findAll();
    }

    public List<Booking> search(UserPrincipal currentUser,
                                String status,
                                String type,
                                String bookingId,
                                String divisionId,
                                Long requester,
                                LocalDate date) {
        // Supervisors see bookings assigned to them OR in their division
        if (currentUser.getRole() == Role.SUPERVISOR) {
            String supervisorId = currentUser.getId();
            System.out.println("=== SUPERVISOR FETCHING BOOKINGS ===");
            System.out.println("Supervisor ID: " + supervisorId);
            System.out.println("Supervisor Division: " + currentUser.getDivisionId());
            
            List<Booking> assignedToMe = spaceRepository.findByAssignedSupervisorId(supervisorId);
            List<Booking> inMyDivision = spaceRepository.findByDivisionId(currentUser.getDivisionId());
            
            // Combine and deduplicate
            Set<Booking> combined = new HashSet<>(assignedToMe);
            combined.addAll(inMyDivision);
            
            System.out.println("Found " + assignedToMe.size() + " bookings assigned to supervisor");
            System.out.println("Found " + inMyDivision.size() + " bookings in supervisor's division");
            System.out.println("Total unique: " + combined.size());
            
            return new ArrayList<>(combined);
        }
        
        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (currentUser.getRole() == Role.PROFESSIONAL) {
                // For professionals, return bookings assigned to them OR requested by them
                Predicate isAssigned = cb.equal(root.get("assignedProfessionalId"), currentUser.getId());
                Predicate isRequester = cb.equal(root.get("requester"), currentUser.getId());
                predicates.add(cb.or(isAssigned, isRequester));
            } else if (currentUser.getRole() == Role.ADMIN) {
                // Admin sees items in admin-owned workflow stages
                // Bookings don't have "Assigned to Supervisor" or "WorkOrder Created" stages
                // IMPORTANT: Include IN_PROGRESS and COMPLETED so admin can see bookings being worked on
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
            }
            
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), Status.fromValue(status)));
            }
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (bookingId != null && !bookingId.isBlank()) {
                predicates.add(cb.equal(root.get("bookingId"), bookingId));
            }
            if (divisionId != null) {
                predicates.add(cb.equal(root.get("divisionId"), divisionId));
            }
            if (requester != null) {
                predicates.add(cb.equal(root.get("requester"), requester));
            }
            if (date != null) {
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
                predicates.add(cb.between(root.get("dateTime"), start, end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return spaceRepository.findAll(spec);
    }

    public Booking supervisorReview(Long id, UserPrincipal supervisor) {
        if (supervisor.getRole() != Role.SUPERVISOR) {
            throw new ApiException("Access denied");
        }
        Booking booking = spaceRepository.findById(id).orElseThrow(() -> new ApiException("Booking not found"));
        if (booking.getDivisionId() == null || !booking.getDivisionId().equals(supervisor.getDivisionId())) {
            throw new ApiException("supervisor sees only division requests");
        }
        booking.setStatus(Status.REVIEWED);
        requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.REVIEWED, supervisor.getId());
        requestLifecycleService.notifyUser(booking.getRequester(), "Booking reviewed", "Booking " + booking.getBookingId() + " reviewed");
        return spaceRepository.save(booking);
    }

    public Booking adminStartReview(Long id, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Booking booking = spaceRepository.findById(id).orElseThrow(() -> new ApiException("Booking not found"));
        booking.setStatus(Status.UNDER_REVIEW);
        requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.UNDER_REVIEW, admin.getId());
        if (booking.getAssignedSupervisorId() != null) {
            requestLifecycleService.notifyUser(booking.getAssignedSupervisorId(), "Booking under review", "Booking " + booking.getBookingId() + " is under review");
        }
        return spaceRepository.save(booking);
    }

    public Booking adminApprove(Long id, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Booking booking = spaceRepository.findById(id).orElseThrow(() -> new ApiException("Booking not found"));
        booking.setStatus(Status.APPROVED);
        requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.APPROVED, admin.getId());
        requestLifecycleService.notifyUser(booking.getRequester(), "Booking approved", "Booking " + booking.getBookingId() + " approved");
        return spaceRepository.save(booking);
    }

    public Booking adminReject(Long id, String reason, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Booking booking = spaceRepository.findById(id).orElseThrow(() -> new ApiException("Booking not found"));
        booking.setStatus(Status.REJECTED);
        booking.setRejectionReason(reason);
        requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.REJECTED, admin.getId());
        String notificationMessage = "Booking " + booking.getBookingId() + " rejected";
        if (reason != null && !reason.isBlank()) {
            notificationMessage += ". Reason: " + reason;
        }
        requestLifecycleService.notifyUser(booking.getRequester(), "Booking rejected", notificationMessage);
        return spaceRepository.save(booking);
    }

    public Booking adminClose(Long id, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Booking booking = spaceRepository.findById(id).orElseThrow(() -> new ApiException("Booking not found"));
        booking.setStatus(Status.CLOSED);
        requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.CLOSED, admin.getId());
        requestLifecycleService.notifyUser(booking.getRequester(), "Booking closed", "Booking " + booking.getBookingId() + " closed");
        return spaceRepository.save(booking);
    }

    
    @Transactional
    public Booking adminAssignProfessional(Long id, String professionalId, String instructions, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Booking booking = spaceRepository.findById(id).orElseThrow(() -> new ApiException("Booking not found"));

        System.out.println("=== ASSIGNING BOOKING TO PROFESSIONAL ===");
        System.out.println("Booking ID: " + booking.getId());
        System.out.println("Booking Business ID: " + booking.getBookingId());
        System.out.println("Professional ID: " + professionalId);
        System.out.println("Current Status: " + booking.getStatus());

        // For bookings, admin can directly assign professional from Under Review
        Status current = booking.getStatus();
        if (current == Status.SUBMITTED) {
            requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.UNDER_REVIEW, admin.getId());
            booking.setStatus(Status.UNDER_REVIEW);
            current = Status.UNDER_REVIEW;
        }
        if (current != Status.UNDER_REVIEW && current != Status.ASSIGNED_TO_PROFESSIONALS) {
            throw new ApiException("Booking must be under review before assigning a professional");
        }
        
        booking.setAssignedProfessionalId(professionalId);
        if (current == Status.UNDER_REVIEW) {
            requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.ASSIGNED_TO_PROFESSIONALS, admin.getId());
        }
        booking.setStatus(Status.ASSIGNED_TO_PROFESSIONALS);
        
        Booking saved = spaceRepository.save(booking);
        
        // Notify the professional about the assignment
        requestLifecycleService.notifyUser(professionalId, "New Booking Assignment", 
            "Booking " + booking.getBookingId() + " has been assigned to you");
        
        System.out.println("New Status: " + saved.getStatus());
        System.out.println("Assigned Professional ID: " + saved.getAssignedProfessionalId());
        System.out.println("Notification sent to professional");
        System.out.println("=== ASSIGNMENT COMPLETE ===");
        
        return saved;
    }

    public Booking adminAssign(Long id, String divisionId, String supervisorId, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Booking booking = spaceRepository.findById(id).orElseThrow(() -> new ApiException("Booking not found"));
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

        booking.setDivisionId(divisionId);
        booking.setAssignedSupervisorId(finalSupervisorId);
        requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.UNDER_REVIEW, admin.getId());
        requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.ASSIGNED_TO_SUPERVISOR, admin.getId());
        booking.setStatus(Status.ASSIGNED_TO_SUPERVISOR);
        requestLifecycleService.notifyUser(finalSupervisorId, "New assignment", "Booking " + booking.getBookingId() + " assigned to you");
        return spaceRepository.save(booking);
    }
    
    @Transactional
    public Booking updateBookingCost(Long id, java.math.BigDecimal materialCost, java.math.BigDecimal laborCost, String partsUsed, UserPrincipal professional) {
        if (professional.getRole() != Role.PROFESSIONAL) {
            throw new ApiException("Access denied");
        }
        Booking booking = spaceRepository.findById(id).orElseThrow(() -> new ApiException("Booking not found"));
        if (!booking.getAssignedProfessionalId().equals(professional.getId())) {
            throw new ApiException("You are not assigned to this booking");
        }
        
        booking.setMaterialCost(materialCost);
        booking.setLaborCost(laborCost);
        booking.setPartsUsed(partsUsed);
        
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        if (materialCost != null) total = total.add(materialCost);
        if (laborCost != null) total = total.add(laborCost);
        booking.setTotalCost(total);
        
        return spaceRepository.save(booking);
    }
    
    @Transactional
    public Booking professionalUpdateStatus(Long id, String statusStr, UserPrincipal professional) {
        if (professional.getRole() != Role.PROFESSIONAL) {
            throw new ApiException("Access denied");
        }
        Booking booking = spaceRepository.findById(id).orElseThrow(() -> new ApiException("Booking not found"));
        if (!booking.getAssignedProfessionalId().equals(professional.getId())) {
            throw new ApiException("You are not assigned to this booking");
        }
        
        Status newStatus;
        if ("In Progress".equals(statusStr)) {
            newStatus = Status.IN_PROGRESS;
        } else if ("Completed".equals(statusStr)) {
            newStatus = Status.COMPLETED;
        } else {
            throw new ApiException("Invalid status");
        }
        
        requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), newStatus, professional.getId());
        booking.setStatus(newStatus);
        
        // Notify admins and supervisor about status change
        // Notify admins (both database and Keycloak) and supervisor about status change
        String notificationTitle = newStatus == Status.IN_PROGRESS ? "Booking work started" : "Booking completed";
        String notificationMessage = "Booking " + booking.getBookingId() + " is now " + newStatus.getValue();
        
        requestLifecycleService.notifyUsersByRole(Role.ADMIN, notificationTitle, notificationMessage);
        
        if (booking.getAssignedSupervisorId() != null) {
            requestLifecycleService.notifyUser(booking.getAssignedSupervisorId(), notificationTitle, notificationMessage);
        }
        
        return spaceRepository.save(booking);
    }
    
    @Transactional
    public void delete(Long id, UserPrincipal currentUser) {
        Booking booking = spaceRepository.findById(id)
                .orElseThrow(() -> new ApiException("Booking not found"));
        
        // Users can only delete their own requests in Submitted status
        // Admins can delete any request
        if (currentUser.getRole() == Role.USER) {
            if (!booking.getRequester().equals(currentUser.getId())) {
                throw new ApiException("You can only delete your own requests");
            }
            if (booking.getStatus() != Status.SUBMITTED) {
                throw new ApiException("You can only delete requests in Submitted status");
            }
        } else if (currentUser.getRole() != Role.ADMIN) {
            throw new ApiException("Only users and admins can delete requests");
        }
        
        System.out.println("=== DELETING BOOKING ===");
        System.out.println("Booking ID: " + booking.getId());
        System.out.println("Booking Business ID: " + booking.getBookingId());
        System.out.println("Deleted by: " + currentUser.getUsername() + " (Role: " + currentUser.getRole() + ")");
        
        spaceRepository.delete(booking);
        
        System.out.println("=== BOOKING DELETED ===");
    }
}
