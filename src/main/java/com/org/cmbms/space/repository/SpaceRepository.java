
package com.org.cmbms.space.repository;

import com.org.cmbms.space.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpaceRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {
    List<Booking> findByDivisionId(String divisionId);
    List<Booking> findByAssignedProfessionalId(String assignedProfessionalId); // Changed to String
    List<Booking> findByAssignedSupervisorId(String assignedSupervisorId); // Changed to String
    List<Booking> findByTypeAndLayoutAndDateTimeBetween(String type, String layout, LocalDateTime start, LocalDateTime end);
    Optional<Booking> findByBookingId(String bookingId);
}
