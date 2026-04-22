package com.org.cmbms.maintenance.dto;

import lombok.Data;

@Data
public class CostUpdateRequest {
    private Long maintenanceRequestId;
    private Double materialCost;
    private Double laborCost;
    private String partsUsed;
}
