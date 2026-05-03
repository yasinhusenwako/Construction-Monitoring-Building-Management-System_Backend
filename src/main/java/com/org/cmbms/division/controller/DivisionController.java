package com.org.cmbms.division.controller;

import com.org.cmbms.division.model.Division;
import com.org.cmbms.division.service.DivisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/divisions")
@RequiredArgsConstructor
public class DivisionController {

    private final DivisionService divisionService;

    /**
     * Get all divisions
     */
    @GetMapping
    public ResponseEntity<List<Division>> getAllDivisions() {
        return ResponseEntity.ok(divisionService.getAllDivisions());
    }

    /**
     * Get division by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Division> getDivisionById(@PathVariable Long id) {
        return ResponseEntity.ok(divisionService.getDivisionById(id));
    }
}
