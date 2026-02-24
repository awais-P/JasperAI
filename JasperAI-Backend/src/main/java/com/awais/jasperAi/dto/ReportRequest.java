package com.awais.jasperAi.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ReportRequest(
        String prompt,
        List<Map<String, Object>> data,
        Boolean save
) {
    public boolean shouldSave() {
        return save != null && save;
    }

    // Now returns exact types: "invoiceNumber (java.lang.String), lineTotal (java.lang.Double)"
    public String getSchema() {
        if (data == null || data.isEmpty()) return "No data provided";

        return data.get(0).entrySet().stream()
                .map(entry -> {
                    String type = (entry.getValue() != null) ?
                            entry.getValue().getClass().getName() :
                            "java.lang.String";
                    return entry.getKey() + " (" + type + ")";
                })
                .collect(Collectors.joining(", "));
    }
}