package com.awais.jasperAi.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.regex.Pattern;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiService() {
        this.objectMapper = new ObjectMapper();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000);
        factory.setReadTimeout(60000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public String generateJrxml(String userPrompt, String modelName) {
        // Use the validated model IDs
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

        String systemInstruction = """
            You are a JasperReports 6.20 expert. Output ONLY valid JRXML.
            
            CRITICAL RULES:
            1. DO NOT add 'uuid' attributes to: <jasperReport>, <property>, <import>, <style>, <field>, <variable>, <parameter>, <queryString>.
            2. ONLY add 'uuid' to visual elements like <reportElement>, <band>, <staticText>, <textField>.
            3. UUID format MUST be 36-char (e.g. "123e4567-e89b-12d3-a456-426614174000"). NO SHORT IDS.
            4. Start directly with <?xml version="1.0" encoding="UTF-8"?>.
            """;

        try {
            ObjectNode rootNode = objectMapper.createObjectNode();
            ArrayNode contentsArray = rootNode.putArray("contents");
            ObjectNode contentNode = contentsArray.addObject();
            ArrayNode partsArray = contentNode.putArray("parts");
            partsArray.addObject().put("text", systemInstruction + "\n\n User Request: " + userPrompt);

            String response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(rootNode))
                    .retrieve()
                    .body(String.class);

            return extractAndSanitizeXml(response);

        } catch (Exception e) {
            // Pass the specific error up so Controller knows if it's 429 or 404
            throw new RuntimeException(e.getMessage());
        }
    }

    private String extractAndSanitizeXml(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.path("candidates").isEmpty()) {
                throw new RuntimeException("AI returned no content: " + json);
            }

            String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            // 1. Strip Markdown
            text = text.replace("```xml", "").replace("```", "").trim();

            // 2. Find XML start
            int xmlStart = text.indexOf("<?xml");
            if (xmlStart != -1) {
                text = text.substring(xmlStart);
            }

            // 3. AUTO-FIX: Remove 'uuid' from metadata tags where it causes errors
            // Regex matches: <tagname ... uuid="..." ...> and removes the uuid part
            String[] forbiddenTags = {"property", "import", "template", "style", "subDataset", "field", "variable", "parameter", "queryString"};

            for (String tag : forbiddenTags) {
                // Remove uuid="..." from specific tags
                text = text.replaceAll("(<" + tag + "[^>]*?)\\s+uuid=\"[^\"]*\"", "$1");
            }

            // 4. AUTO-FIX: Remove 'size' attribute from <style> (common hallucination)
            text = text.replaceAll("(<style[^>]*?)\\s+size=\"[^\"]*\"", "$1");

            return text;

        } catch (Exception e) {
            throw new RuntimeException("Error parsing AI response: " + e.getMessage());
        }
    }
}