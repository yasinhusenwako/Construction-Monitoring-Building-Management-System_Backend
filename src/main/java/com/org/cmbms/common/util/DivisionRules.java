package com.org.cmbms.common.util;

import com.org.cmbms.common.exception.ApiException;

import java.util.LinkedHashSet;
import java.util.Set;

public final class DivisionRules {

    private static final Set<String> ALLOWED_DIVISION_IDS = Set.of("DIV-001", "DIV-002", "DIV-003");

    private DivisionRules() {
    }

    public static boolean isAllowed(String divisionId) {
        String normalized = normalize(divisionId);
        return normalized != null && ALLOWED_DIVISION_IDS.contains(normalized);
    }

    public static void assertAllowed(String divisionId) {
        if (!isAllowed(divisionId)) {
            throw new ApiException("Division must be one of: DIV-001, DIV-002, DIV-003");
        }
    }

    public static String normalize(String divisionId) {
        if (divisionId == null) return null;
        String raw = divisionId.trim();
        if (raw.isEmpty()) return null;

        String upper = raw.toUpperCase().replace('_', '-');
        if (upper.startsWith("DIV-")) {
            String suffix = upper.substring(4).trim();
            if (suffix.matches("\\d+")) {
                return "DIV-" + String.format("%03d", Integer.parseInt(suffix));
            }
            return null;
        }

        if (upper.startsWith("DIV") && upper.substring(3).matches("\\d+")) {
            return "DIV-" + String.format("%03d", Integer.parseInt(upper.substring(3)));
        }

        if (upper.matches("\\d+")) {
            return "DIV-" + String.format("%03d", Integer.parseInt(upper));
        }

        return null;
    }

    public static boolean matches(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);
        return a != null && b != null && a.equals(b);
    }

    public static Set<String> aliases(String divisionId) {
        String normalized = normalize(divisionId);
        if (normalized == null) return Set.of();

        String numeric = String.valueOf(Integer.parseInt(normalized.substring(4)));
        Set<String> values = new LinkedHashSet<>();
        values.add(normalized); // DIV-001
        values.add(numeric);    // 1
        return values;
    }
}
