package com.awais.jasperAi.dto;

import java.util.List;
import java.util.Map;

public record ReportRequest(
        String prompt,
        List<Map<String, Object>> data,
        Boolean save // Change 'boolean' to 'Boolean' (Capital B)
) {
    // Add a helper method to handle nulls safely
    public boolean shouldSave() {
        return save != null && save;
    }
}