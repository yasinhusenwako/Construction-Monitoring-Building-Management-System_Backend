
package com.org.cmbms.space.service;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.enums.RequestType;
import com.org.cmbms.common.enums.Status;
import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.space.dto.BookingRequestDTO;
import com.org.cmbms.space.model.Booking;
import com.org.cmbms.space.repository.SpaceRepository;
import com.org.cmbms.user.model.User;
import com.org.cmbms.user.repository.UserRepository;
import com.org.cmbms.workflow.service.RequestLifecycleService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
        if (currentUser.getRole() == Role.SUPERVISOR && currentUser.getDivisionId() != null) {
            booking.setDivisionId(currentUser.getDivisionId());
        } else {
            booking.setDivisionId(dto.getDivisionId());
        }
        Booking saved = spaceRepository.save(booking);
        requestLifecycleService.initialize(RequestType.BOOKING, saved.getId(), currentUser.getId());
        return saved;
    }

    public List<Booking> all(UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.PROFESSIONAL) {
            throw new ApiException("Professional cannot access bookings");
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
            throw new ApiException("Professional cannot access bookings");
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

    public Booking adminAssign(Long id, Long divisionId, Long supervisorId, UserPrincipal admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        Booking booking = spaceRepository.findById(id).orElseThrow(() -> new ApiException("Booking not found"));
        User supervisor = userRepository.findById(supervisorId).orElseThrow(() -> new ApiException("Supervisor not found"));
        if (supervisor.getRole() != Role.SUPERVISOR) {
            throw new ApiException("Selected user is not a supervisor");
        }
        if (supervisor.getDivisionId() == null || !supervisor.getDivisionId().equals(divisionId)) {
            throw new ApiException("supervisor must belong to division");
        }
        booking.setDivisionId(divisionId);
        booking.setAssignedSupervisorId(supervisorId);
        requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.UNDER_REVIEW, admin.getId());
        requestLifecycleService.transition(RequestType.BOOKING, booking.getId(), Status.ASSIGNED_TO_SUPERVISOR, admin.getId());
        booking.setStatus(Status.ASSIGNED_TO_SUPERVISOR);
        requestLifecycleService.notifyUser(supervisorId, "New assignment", "Booking " + booking.getBookingId() + " assigned to you");
        return spaceRepository.save(booking);
    }
}
