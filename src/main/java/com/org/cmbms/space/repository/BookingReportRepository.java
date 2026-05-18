package com.org.cmbms.space.repository;

import com.org.cmbms.space.model.BookingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingReportRepository extends JpaRepository<BookingReport, Long> {
    
    /**
     * Find all reports for a specific assignment, ordered by most recent first
     */
    List<BookingReport> findByAssignmentIdOrderByCreatedAtDesc(Long assignmentId);
    
    /**
     * Find all reports for a specific assignment
     */
    List<BookingReport> findByAssignmentId(Long assignmentId);
    
    /**
     * Find all reports for a specific booking (via assignment)
     */
    @Query("SELECT br FROM BookingReport br " +
           "JOIN BookingAssignment ba ON br.assignmentId = ba.id " +
           "WHERE ba.bookingId = :bookingId " +
           "ORDER BY br.createdAt DESC")
    List<BookingReport> findByBookingId(@Param("bookingId") Long bookingId);
    
    /**
     * Find all reports submitted by a specific professional
     */
    @Query("SELECT br FROM BookingReport br " +
           "JOIN BookingAssignment ba ON br.assignmentId = ba.id " +
           "WHERE ba.professionalId = :professionalId " +
           "ORDER BY br.createdAt DESC")
    List<BookingReport> findByProfessionalId(@Param("professionalId") String professionalId);
}
