package com.org.cmbms.common.util;

import com.org.cmbms.common.exception.ApiException;

import java.util.Set;

public final class DivisionRules {

    private static final Set<String> ALLOWED_DIVISION_IDS = Set.of("DIV-001", "DIV-002", "DIV-003");

    private DivisionRules() {
    }

    public static boolean isAllowed(String divisionId) {
        return divisionId != null && ALLOWED_DIVISION_IDS.contains(divisionId);
    }

    public static void assertAllowed(String divisionId) {
        if (!isAllowed(divisionId)) {
            throw new ApiException("Division must be one of: DIV-001, DIV-002, DIV-003");
        }
    }
}
