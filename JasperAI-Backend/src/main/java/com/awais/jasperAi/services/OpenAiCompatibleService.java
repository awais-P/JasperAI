package com.awais.jasperAi.services;

import com.awais.jasperAi.utils.JrxmlSanitizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

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

        // Create a String converter that accepts EVERYTHING
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setSupportedMediaTypes(List.of(MediaType.ALL));

        this.restClient = builder
                .requestFactory(factory)
                // --- SPRING 7.0 FIX: Use the new configuration API ---
                .configureMessageConverters(configurer -> {
                    // addCustomConverter automatically places this ahead of the default converters
                    configurer.addCustomConverter(stringConverter);
                })
                .build();
    }

    public String generate(String providerName, String apiKey, String baseUrl, String model, String prompt, String exampleXml) {
        try {
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("model", model);
            rootNode.put("temperature", 0.1);
            String combinedSystemPrompt = """
                You are a professional JasperReports 6.20 designer. Output ONLY raw JRXML code.
                
                CRITICAL SYNTAX RULES (DO NOT BREAK THESE):
                1. Look at the DATA SCHEMA. For every <field>, you MUST use the exact 'class' provided in parentheses (e.g., java.lang.Double, java.lang.String). Do not assume BigDecimal.
                2. NEVER declare standard parameters like 'REPORT_CONNECTION'.
                3. NEVER use the 'style' attribute inside a <textField> or <staticText>.
                4. NEVER use 'columnHeight' inside the root <jasperReport> tag.
                5. NEVER use 'isSplitAllowed' or 'splitType' on <band> elements.
                6. The root <jasperReport> MUST NOT have a 'uuid' attribute.
                7. Ensure every <textFieldExpression> uses <![CDATA[ ... ]]>.
                
                DESIGN GUIDELINES:
                1. Use <box> elements with pen lineStyle="Solid" for table cells to create borders.
                2. Use <reportElement mode="Opaque" backcolor="#EEEEEE"> for headers.
                
                LEARNING SHOTS (Follow these perfect structures):
                """ + (exampleXml.isEmpty() ? "No examples provided." : exampleXml);

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