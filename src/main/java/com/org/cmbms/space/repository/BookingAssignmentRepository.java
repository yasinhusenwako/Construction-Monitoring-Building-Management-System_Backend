package com.org.cmbms.space.repository;

import com.org.cmbms.space.model.BookingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingAssignmentRepository extends JpaRepository<BookingAssignment, Long> {
    
    // Get all assignments for a specific booking
    List<BookingAssignment> findByBookingId(Long bookingId);
    
    // Get all active (non-inactive) assignments for a specific booking
    @Query("SELECT ba FROM BookingAssignment ba WHERE ba.bookingId = :bookingId AND ba.status <> 'INACTIVE'")
    List<BookingAssignment> findActiveAssignmentsByBookingId(@Param("bookingId") Long bookingId);
    
    // Get all assignments for a specific professional
    List<BookingAssignment> findByProfessionalId(String professionalId);
    
    // Get all active (non-inactive) assignments for a specific professional
    @Query("SELECT ba FROM BookingAssignment ba WHERE ba.professionalId = :professionalId AND ba.status <> 'INACTIVE'")
    List<BookingAssignment> findActiveAssignmentsByProfessionalId(@Param("professionalId") String professionalId);
    
    // Check if a professional is already assigned to a booking
    boolean existsByBookingIdAndProfessionalId(Long bookingId, String professionalId);
    
    // Get specific assignment
    Optional<BookingAssignment> findByBookingIdAndProfessionalId(Long bookingId, String professionalId);
}
