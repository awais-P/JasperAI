package com.awais.jasperAi.utils;

public class JrxmlSanitizer {
    public static String sanitize(String xml) {
        if (xml == null) return "";

        // 1. Strip Markdown Code Blocks
        xml = xml.replaceAll("(?s)```xml(.*?)```", "$1");
        xml = xml.replaceAll("(?s)```(.*?)```", "$1");

        // 2. ENFORCE THE NAME ATTRIBUTE (Fixes the Llama crash)
        // If the <jasperReport> tag doesn't have a name attribute, add a default one.
        if (xml.contains("<jasperReport") && !xml.contains("name=\"")) {
            xml = xml.replaceFirst("<jasperReport", "<jasperReport name=\"AI_Generated_Report\"");
        }

        // 3. Strip Illegal Attributes
        xml = xml.replaceAll(" uuid=\"[a-zA-Z0-9-]+\"", "");
        xml = xml.replaceAll(" backcolor=\"[a-zA-Z0-9-#]+\"", "");
        xml = xml.replaceAll(" style=\"[^\"]+\"", "");
        xml = xml.replaceAll(" splitType=\"[^\"]+\"", "");
        xml = xml.replaceAll(" isSplitAllowed=\"[^\"]+\"", "");
        xml = xml.replaceAll(" columnHeight=\"[^\"]+\"", "");

        // 4. Strip duplicate built-in parameters
        xml = xml.replaceAll("<parameter name=\"REPORT_CONNECTION\".*?/>", "");
        xml = xml.replaceAll("<parameter name=\"REPORT_CONNECTION\".*?</parameter>", "");

        return xml.trim();
    }
}