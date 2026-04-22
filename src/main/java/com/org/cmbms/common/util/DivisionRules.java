package com.org.cmbms.common.util;

import com.org.cmbms.common.exception.ApiException;

import java.util.Set;

public final class DivisionRules {

    private static final Set<Long> ALLOWED_DIVISION_IDS = Set.of(1L, 2L, 3L);

    private DivisionRules() {
    }

    public static boolean isAllowed(Long divisionId) {
        return divisionId != null && ALLOWED_DIVISION_IDS.contains(divisionId);
    }

    public static void assertAllowed(Long divisionId) {
        if (!isAllowed(divisionId)) {
            throw new ApiException("Division must be one of: 1, 2, 3");
        }
    }
}