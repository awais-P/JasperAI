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

import java.nio.charset.StandardCharsets;

@Service
public class OpenAiCompatibleService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleService(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(90_000);
        factory.setReadTimeout(120_000);

        this.restClient = builder.requestFactory(factory).build();
    }

    public String generate(String providerName, String apiKey, String baseUrl,
                           String model, String prompt, String exampleXml) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);
            root.put("temperature", 0.1);
            root.put("max_tokens", 8192); // 🔴 FIX: Cap tokens — prevents 402 on free tier

            String systemPrompt = buildSystemPrompt(exampleXml);

            ArrayNode messages = root.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", prompt);

            byte[] responseBytes = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.ALL)
                    .body(objectMapper.writeValueAsString(root))
                    .retrieve()
                    .body(byte[].class);

            String response = new String(responseBytes, StandardCharsets.UTF_8);
            JsonNode responseJson = objectMapper.readTree(response);

            if (!responseJson.has("choices") || responseJson.path("choices").isEmpty()) {
                throw new RuntimeException("Unexpected response: " + response);
            }

            String content = responseJson.path("choices").get(0)
                    .path("message").path("content").asText();

            return JrxmlSanitizer.sanitize(content);

        } catch (Exception e) {
            throw new RuntimeException("[" + providerName + "] Call Failed: " + e.getMessage());
        }
    }

    private String buildSystemPrompt(String exampleXml) {
        return """
            You are a JasperReports 6.21 JRXML expert. Output ONLY raw JRXML. No markdown. No explanation.
            
            ╔══════════════════════════════════╗
            ║   ABSOLUTE RULES — NEVER BREAK   ║
            ╚══════════════════════════════════╝
            
            RULE 1 — FONT COLOR:
              ✅ CORRECT: <reportElement forecolor="#FF0000" .../>
              ❌ WRONG:   <font forecolor="#FF0000"/>
              ❌ WRONG:   <font color="#FF0000"/>
              The <font> tag ONLY accepts: fontName, size, isBold, isItalic, isUnderline, isPdfEmbedded, pdfFontName
            
            RULE 2 — BOX PLACEMENT:
              ✅ CORRECT: <textField><reportElement.../><box>...</box><textFieldExpression.../></textField>
              ❌ WRONG:   <band height="20"><box>...</box></band>
              <box> is ONLY valid INSIDE <textField> or <staticText>. NEVER as a direct child of <band>.
            
            RULE 3 — BAND HEIGHT:
              Every element's (y + height) MUST be <= the band's height attribute.
              If band height="50", no element can have y=40 height=20 (that would be 60 > 50).
            
            RULE 4 — ROOT ELEMENT:
              ✅ CORRECT: <jasperReport name="Report" ...>
              ❌ WRONG:   <jasperReport uuid="abc-123" ...>
              NEVER include uuid attribute on root. ALWAYS include name attribute.
            
                RULE 5 — DEPRECATED ATTRIBUTES:
                  ❌ NEVER use: isSplitAllowed on bands.
                  ❌ NEVER use: splitType on bands.
                  ❌ NEVER use: isStretchWithOverflow — use textAdjust="StretchHeight" instead
                  ❌ NEVER use: style="" on textField or staticText
            RULE 6 — EXPRESSIONS:
              ✅ CORRECT: <textFieldExpression><![CDATA[$F{fieldName}]]></textFieldExpression>
              Always wrap expressions in CDATA blocks.
            
            DESIGN STANDARDS:
            - Use <box><pen lineStyle="Solid" lineWidth="0.5"/></box> inside cells for borders.
            - Use <reportElement mode="Opaque" backcolor="#4A90D9" forecolor="#FFFFFF"/> for headers.
            - Standard page: width="595" height="842" (A4). Column width = pageWidth - margins.
            
            """ + (exampleXml.isBlank() ? "No examples loaded." : "REFERENCE EXAMPLES:\n" + exampleXml);
    }
}