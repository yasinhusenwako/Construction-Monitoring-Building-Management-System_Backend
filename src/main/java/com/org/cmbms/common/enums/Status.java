
package com.org.cmbms.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under Review"),
    ASSIGNED_TO_SUPERVISOR("Assigned to Supervisor"),
    ASSIGNED_TO_PROFESSIONALS("Assigned to Professionals"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    REVIEWED("Reviewed"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CLOSED("Closed");

    private final String value;

    Status(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Status fromValue(String raw) {
        if (raw == null) {
            return null;
        }
        for (Status status : values()) {
            if (status.value.equalsIgnoreCase(raw) || status.name().equalsIgnoreCase(raw)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + raw);
    }
}
