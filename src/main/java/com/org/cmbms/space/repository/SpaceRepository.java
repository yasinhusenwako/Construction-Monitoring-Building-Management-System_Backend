
package com.org.cmbms.space.repository;

import com.org.cmbms.space.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface SpaceRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {
    List<Booking> findByDivisionId(Long divisionId);
    List<Booking> findByAssignedProfessionalId(Long assignedProfessionalId);
    List<Booking> findByTypeAndLayoutAndDateTimeBetween(String type, String layout, LocalDateTime start, LocalDateTime end);
}
