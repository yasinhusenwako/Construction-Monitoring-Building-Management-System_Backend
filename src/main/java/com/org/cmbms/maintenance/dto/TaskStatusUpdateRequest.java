package com.org.cmbms.maintenance.dto;

import com.org.cmbms.common.enums.Status;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskStatusUpdateRequest {
    @NotNull
    private Status status;
}

