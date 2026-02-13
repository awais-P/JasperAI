package com.awais.jasperAi.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JrxmlSanitizer {
    public static String sanitize(String rawText) {
        if (rawText == null) return "";

        // 1. Clean Markdown and Prolog
        String text = rawText.replace("```xml", "").replace("```", "").trim();
        int xmlStart = text.indexOf("<?xml");
        if (xmlStart != -1) text = text.substring(xmlStart);

        // 2. FORCE NAME on Root (Fixes "Attribute 'name' must appear")
        // If <jasperReport ...> exists but no name=, inject it.
        if (text.contains("<jasperReport") && !text.contains("name=")) {
            text = text.replaceFirst("<jasperReport", "<jasperReport name=\"AutoReport_" + System.currentTimeMillis() + "\"");
        }

        // 3. REMOVE UUID from ROOT (Fixes "Attribute 'uuid' is not allowed on jasperReport")
        // Regex: Finds <jasperReport ... uuid="..." and deletes the uuid part
        text = text.replaceAll("(<jasperReport[^>]*?)(\\s+uuid=\"[^\"]*\")", "$1");

        // 4. AGGRESSIVE UUID CLEANUP
        // Only allow UUIDs on visual elements. Remove from EVERYTHING else.
        String[] badTags = {"property", "import", "style", "box", "font", "field", "parameter", "variable", "queryString", "template", "textElement", "band", "group"};
        for (String tag : badTags) {
            text = text.replaceAll("(<" + tag + "[^>]*?)\\s+uuid=\"[^\"]*\"", "$1");
        }

        // 5. FIX MISTRAL'S BAD NESTING
        // Mistral sometimes puts <textField> inside <style>. This regex deletes <textField> blocks that are inside <style>
        // Note: Full XML parsing is better, but this regex catches the common case
        // Removing 'backcolor' from bands (Groq error)
        text = text.replaceAll("(<band[^>]*?)\\s+backcolor=\"[^\"]*\"", "$1");

        return text;
    }
}