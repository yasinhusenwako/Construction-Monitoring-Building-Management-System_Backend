
package com.org.cmbms.space.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.space.dto.BookingRequestDTO;
import com.org.cmbms.space.model.Booking;
import com.org.cmbms.space.service.SpaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    @PostMapping
    public ResponseEntity<Booking> create(@Valid @RequestBody BookingRequestDTO request) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(spaceService.create(request, currentUser));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Booking> update(@PathVariable Long id, @RequestBody BookingRequestDTO request) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(spaceService.update(id, request, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<Booking>> all(@RequestParam(required = false) String status,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) String bookingId,
                                             @RequestParam(required = false) Long divisionId,
                                             @RequestParam(required = false) Long requester,
                                             @RequestParam(required = false) LocalDate date) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(spaceService.search(currentUser, status, type, bookingId, divisionId, requester, date));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<Booking> review(@PathVariable Long id) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(spaceService.supervisorReview(id, currentUser));
    }

    @PatchMapping("/{id}/review")
    public ResponseEntity<Booking> adminStartReview(@PathVariable Long id) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        try {
            return ResponseEntity.ok(spaceService.adminStartReview(id, currentUser));
        } catch (Exception ex) {
            throw new com.org.cmbms.common.exception.ApiException(ex.getMessage());
        }
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<Booking> approve(@PathVariable Long id) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(spaceService.adminApprove(id, currentUser));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<Booking> reject(@PathVariable Long id) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(spaceService.adminReject(id, currentUser));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<Booking> close(@PathVariable Long id) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(spaceService.adminClose(id, currentUser));
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<Booking> updateStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        String status = body.get("status");
        return ResponseEntity.ok(spaceService.professionalUpdateStatus(id, status, currentUser));
    }
}
