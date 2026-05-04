package com.org.cmbms.division.service;

import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.division.model.Division;
import com.org.cmbms.division.repository.DivisionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DivisionService {

    private final DivisionRepository divisionRepository;

    /**
     * Get all divisions
     */
    public List<Division> getAllDivisions() {
        log.info("Fetching all divisions");
        return divisionRepository.findAll();
    }

    /**
     * Get division by ID
     */
    public Division getDivisionById(Long id) {
        log.info("Fetching division with id: {}", id);
        return divisionRepository.findById(id)
                .orElseThrow(() -> new ApiException("Division not found with id: " + id));
    }
}
