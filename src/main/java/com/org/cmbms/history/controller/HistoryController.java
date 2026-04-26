package com.org.cmbms.history.controller;

import com.org.cmbms.history.model.RequestHistory;
import com.org.cmbms.history.service.RequestHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {
    private final RequestHistoryService requestHistoryService;

    @GetMapping("/{type}/{id}")
    public ResponseEntity<List<RequestHistory>> getHistory(@PathVariable String type, @PathVariable Long id) {
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
}
