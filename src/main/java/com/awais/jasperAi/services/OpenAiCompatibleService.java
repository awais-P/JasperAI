package com.awais.jasperAi.services;

import com.awais.jasperAi.utils.JrxmlSanitizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OpenAiCompatibleService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // INJECT THE BUILDER (Solves the Converter Deprecation warnings)
    public OpenAiCompatibleService(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000);
        factory.setReadTimeout(60000);

        this.restClient = builder
                .requestFactory(factory)
                // We do NOT manually set messageConverters here anymore.
                // We trust the Spring Boot default converters.
                .build();
    }

    public String generate(String providerName, String apiKey, String baseUrl, String model, String prompt, String exampleXml) {
        try {
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("model", model);
            rootNode.put("temperature", 0.1);
            String combinedSystemPrompt = """
            You are a JasperReports 6.20 expert. Output ONLY raw JRXML code.
            
            STRICT SYNTAX RULES:
            1. The root <jasperReport> tag MUST have a 'name' attribute.
            2. The root <jasperReport> tag MUST NOT have a 'uuid' attribute.
            3. <band> tags MUST NOT have a 'backcolor' attribute.
            4. <textField> elements MUST be inside a <band>. NEVER inside a <style>.
            5. Do NOT use 'uuid' on <property>, <import>, <style>, or <field>.
            6. Start response with <?xml version="1.0" encoding="UTF-8"?>.

            LEARNING SHOT (Follow this perfect structure):
            """ + (exampleXml.isEmpty() ? "No example provided. Follow standard 6.20 schema." : exampleXml);

            ArrayNode messages = rootNode.putArray("messages");
            messages.addObject().put("role", "system").put("content", combinedSystemPrompt);
            messages.addObject().put("role", "user").put("content", prompt);
            // --- OPTIMIZATION END ---

            // CALL API
            String response = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.ALL) // Accept ANY format (Text, JSON, Binary)
                    .body(objectMapper.writeValueAsString(rootNode))
                    .retrieve()
                    .body(String.class); // Force read as String

            // Manual Parsing
            JsonNode root = objectMapper.readTree(response);

            String content = "";
            if (root.has("choices") && !root.path("choices").isEmpty()) {
                content = root.path("choices").get(0).path("message").path("content").asText();
            } else {
                throw new RuntimeException("Unexpected response: " + response);
            }

            return JrxmlSanitizer.sanitize(content);

        } catch (Exception e) {
            throw new RuntimeException("[" + providerName + "] Call Failed: " + e.getMessage());
        }
    }
}