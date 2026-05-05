package com.org.cmbms.history.controller;

import com.org.cmbms.history.model.RequestHistory;
import com.org.cmbms.history.repository.RequestHistoryRepository;
import com.org.cmbms.history.service.RequestHistoryService;
import com.org.cmbms.user.service.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {
    private final RequestHistoryService requestHistoryService;
    private final RequestHistoryRepository requestHistoryRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final DataSource dataSource;

    @GetMapping("/{type}/{id}")
    public ResponseEntity<List<RequestHistory>> getHistory(@PathVariable String type, @PathVariable Long id) {
        return ResponseEntity.ok(requestHistoryService.getHistory(id, type));
    }

    /**
     * Debug: get raw history without auth (temporary)
     */
    @GetMapping("/debug/{type}/{id}")
    public ResponseEntity<List<RequestHistory>> getHistoryDebug(@PathVariable String type, @PathVariable Long id) {
        return ResponseEntity.ok(requestHistoryService.getHistory(id, type));
    }

    @PostMapping("/note")
    public ResponseEntity<Void> addNote(@RequestParam String type, 
                                       @RequestParam Long requestId, 
                                       @RequestParam Long actorId, 
                                       @RequestParam String note) {
        requestHistoryService.recordHistory(requestId, type, "Note Added", null, null, actorId, note);
        return ResponseEntity.ok().build();
    }

    /**
     * Backfill actor names for history records that show "System"
     * Looks up the request creator from the source table and resolves their display name from Keycloak
     */
    @PostMapping("/backfill-actors")
    public ResponseEntity<Map<String, Object>> backfillActors() {
        int updated = 0;
        int failed = 0;

        try (Connection conn = dataSource.getConnection()) {
            // Find all history records with actorName = 'System'
            List<RequestHistory> systemRecords = requestHistoryRepository.findAll()
                .stream()
                .filter(h -> "System".equals(h.getActorName()))
                .toList();

            for (RequestHistory record : systemRecords) {
                try {
                    String createdBy = resolveCreatedBy(conn, record.getRequestId(), record.getRequestType());
                    if (createdBy != null && !createdBy.isEmpty()) {
                        String displayName = keycloakAdminService.getUserDisplayName(createdBy);
                        record.setActorName(displayName);
                        requestHistoryRepository.save(record);
                        updated++;
                    }
                } catch (Exception e) {
                    failed++;
                }
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage(), "updated", updated, "failed", failed));
        }

        return ResponseEntity.ok(Map.of("updated", updated, "failed", failed, "message", "Backfill complete"));
    }

    private String resolveCreatedBy(Connection conn, Long requestId, String requestType) throws Exception {
        String table = switch (requestType.toUpperCase()) {
            case "PROJECT" -> "projects";
            case "BOOKING" -> "bookings";
            case "MAINTENANCE" -> "maintenance_requests";
            default -> null;
        };
        if (table == null) return null;

        String createdByCol = "BOOKING".equalsIgnoreCase(requestType) ? "requester" : "createdBy";
        String sql = "SELECT \"" + createdByCol + "\" FROM " + table + " WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }
}
