package com.awais.jasperAi.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JrxmlSanitizer {

    public static String sanitize(String xml) {
        if (xml == null) return "";

        // 1. Strip Markdown code fences
        xml = xml.replaceAll("(?s)```xml(.*?)```", "$1").trim();
        xml = xml.replaceAll("(?s)```(.*?)```", "$1").trim();

        // 2. Strip illegal root-level attributes
        xml = xml.replaceAll(" uuid=\"[a-zA-Z0-9-]+\"", "");
        xml = xml.replaceAll(" splitType=\"[^\"]+\"", "");
        xml = xml.replaceAll(" isSplitAllowed=\"[^\"]+\"", "");
        xml = xml.replaceAll(" columnHeight=\"[^\"]+\"", "");
        xml = xml.replaceAll(" isStretchWithOverflow=\"[^\"]+\"", "");

        // 3. Enforce name attribute on <jasperReport>
        xml = ensureNameAttribute(xml);

        // 4. 🔴 FIX: Remove forecolor/color from <font> tags
        //    <font ... forecolor="..." .../>  →  <font ... />
        xml = xml.replaceAll("(<font\\b[^>]*?)\\s+forecolor=\"[^\"]*\"([^>]*?/>)", "$1$2");
        xml = xml.replaceAll("(<font\\b[^>]*?)\\s+color=\"[^\"]*\"([^>]*?/>)", "$1$2");
        // Handle multi-pass in case both appear
        xml = xml.replaceAll("(<font\\b[^>]*?)\\s+forecolor=\"[^\"]*\"([^>]*?/>)", "$1$2");
        xml = xml.replaceAll("(<font\\b[^>]*?)\\s+color=\"[^\"]*\"([^>]*?/>)", "$1$2");

        // 5. 🔴 FIX: Remove <box> that is a direct child of <band>
        //    Pattern: <band ...> ... <box>...</box> ... </band>
        //    We remove standalone <box> blocks not inside a textField/staticText
        xml = removeBoxFromBand(xml);

        // 6. Remove duplicate built-in parameters
        xml = xml.replaceAll("(?s)<parameter name=\"REPORT_CONNECTION\"[^/]*/?>", "");
        xml = xml.replaceAll("(?s)<parameter name=\"REPORT_PARAMETERS_MAP\"[^/]*/?>", "");

        // 7. Strip style attribute on text elements (causes schema error)
        xml = xml.replaceAll("(<(?:textField|staticText)\\b[^>]*?)\\s+style=\"[^\"]*\"([^>]*?>)", "$1$2");

        return xml.trim();
    }

    private static String ensureNameAttribute(String xml) {
        int tagStart = xml.indexOf("<jasperReport");
        int tagEnd = xml.indexOf(">", tagStart);
        if (tagStart == -1 || tagEnd == -1) return xml;

        String tag = xml.substring(tagStart, tagEnd);
        if (!tag.contains("name=")) {
            // Insert name after <jasperReport
            xml = xml.substring(0, tagStart + "<jasperReport".length())
                    + " name=\"AI_Generated_Report\""
                    + xml.substring(tagStart + "<jasperReport".length());
        }
        return xml;
    }

    /**
     * Removes <box>...</box> blocks that appear as direct children of <band>.
     * A <box> is "direct child of band" if it's not inside a textField or staticText.
     * Strategy: find <band> blocks and strip any <box> at the top level within them.
     */
    private static String removeBoxFromBand(String xml) {
        // Simple approach: remove <box> elements that appear immediately after a <band ...> tag
        // or after another element closing tag within a band, but NOT inside textField/staticText.
        // We use a pattern to find <box...>...</box> blocks that are direct children of bands.

        // Robust approach: strip any <box> block that is NOT preceded by <textField or <staticText
        // within the same tag context. Since XML parsing is complex in regex, we use a safe heuristic:
        // Remove <box> blocks that follow a ">" directly (meaning they start a new element after a band or element close)
        // but ONLY if the preceding non-whitespace open tag is <band.

        // Simpler regex: match <box> that appears right after <band attributes> with only whitespace before it
        Pattern bandBoxPattern = Pattern.compile(
                "(<band\\b[^>]*>\\s*)(<box\\b[^>]*>.*?</box>\\s*)",
                Pattern.DOTALL
        );
        Matcher m = bandBoxPattern.matcher(xml);
        xml = m.replaceAll("$1<!-- box removed: not valid as direct band child -->\n");

        return xml;
    }
}