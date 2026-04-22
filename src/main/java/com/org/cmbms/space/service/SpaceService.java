
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
import java.util.List;

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
            int availableInventory = 25;
            if (dto.getCapacity() != null && dto.getCapacity() > 25) {
                throw new ApiException("Office inventory insufficient for requested capacity");
            }
            if (dto.getCapacity() != null && dto.getCapacity() > availableInventory) {
                throw new ApiException("Office inventory insufficient for requested capacity");
            }
        }
        Booking booking = new Booking();
        booking.setBookingId(dto.getBookingId());
        booking.setType(dto.getType());
        booking.setStatus(Status.SUBMITTED);
        booking.setRequester(dto.getRequester());
        booking.setDateTime(dto.getDateTime());
        booking.setCapacity(dto.getCapacity());
        booking.setLayout(dto.getLayout());
        booking.setAmenities(dto.getAmenities());
        Long selectedDivisionId;
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
        return saved;
    }

    public List<Booking> all(UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.PROFESSIONAL) {
            return spaceRepository.findByAssignedProfessionalId(currentUser.getId());
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
                                Long divisionId,
                                Long requester,
                                LocalDate date) {
        if (currentUser.getRole() == Role.PROFESSIONAL) {
            // For professionals, only return bookings assigned to them
            return spaceRepository.findByAssignedProfessionalId(currentUser.getId());
        }
        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
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
            if (currentUser.getRole() == Role.SUPERVISOR && currentUser.getDivisionId() != null) {
                predicates.add(cb.equal(root.get("divisionId"), currentUser.getDivisionId()));
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

    public Booking adminReject(Long id, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Booking booking = spaceRepository.findById(id).orElseThrow(() -> new ApiException("Booking not found"));
        booking.setStatus(Status.REJECTED);
        requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.REJECTED, admin.getId());
        requestLifecycleService.notifyUser(booking.getRequester(), "Booking rejected", "Booking " + booking.getBookingId() + " rejected");
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
    public Booking adminAssignProfessional(Long id, Long professionalId, String instructions, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Booking booking = spaceRepository.findById(id).orElseThrow(() -> new ApiException("Booking not found"));
        User professional = userRepository.findById(professionalId).orElseThrow(() -> new ApiException("Professional not found"));
        if (professional.getRole() != Role.PROFESSIONAL) {
            throw new ApiException("Selected user is not a professional");
        }

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
        requestLifecycleService.notifyUser(professionalId, "New assignment", "Booking " + booking.getBookingId() + " assigned to you");
        return spaceRepository.save(booking);
    }

    public Booking adminAssign(Long id, Long divisionId, Long supervisorId, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Booking booking = spaceRepository.findById(id).orElseThrow(() -> new ApiException("Booking not found"));
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

        booking.setDivisionId(divisionId);
        booking.setAssignedSupervisorId(supervisor.getId());
        requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.UNDER_REVIEW, admin.getId());
        requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.ASSIGNED_TO_SUPERVISOR, admin.getId());
        booking.setStatus(Status.ASSIGNED_TO_SUPERVISOR);
        requestLifecycleService.notifyUser(supervisor.getId(), "New assignment", "Booking " + booking.getBookingId() + " assigned to you");
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
        return spaceRepository.save(booking);
    }
}
