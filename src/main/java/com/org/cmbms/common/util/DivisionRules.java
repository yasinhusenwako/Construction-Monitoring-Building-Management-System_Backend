package com.org.cmbms.common.util;

import com.org.cmbms.common.exception.ApiException;

import java.util.Set;

public final class DivisionRules {

    private static final Set<String> ALLOWED_DIVISION_IDS = Set.of("DIV-001", "DIV-002", "DIV-003");

    private DivisionRules() {
    }

    /**
     * Normalize a divisionId to the canonical "DIV-00X" format.
     * Accepts both "1" and "DIV-001" style inputs.
     */
    public static String normalize(String divisionId) {
        if (divisionId == null) return null;
        String trimmed = divisionId.trim();
        // Already in canonical form
        if (trimmed.startsWith("DIV-")) return trimmed;
        // Plain numeric: "1" → "DIV-001"
        try {
            int num = Integer.parseInt(trimmed);
            return String.format("DIV-%03d", num);
        } catch (NumberFormatException e) {
            return trimmed; // Return as-is; assertAllowed will reject it
        }
    }

    public static boolean isAllowed(String divisionId) {
        return divisionId != null && ALLOWED_DIVISION_IDS.contains(normalize(divisionId));
    }

    public static void assertAllowed(String divisionId) {
        if (!isAllowed(divisionId)) {
            throw new ApiException("Division must be one of: DIV-001, DIV-002, DIV-003");
        }
    }
}
