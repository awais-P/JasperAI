package com.awais.jasperAi.services;

import com.awais.jasperAi.entities.ReportTemplate;
import com.awais.jasperAi.repositories.ReportTemplateRepository;
import com.awais.jasperAi.utils.JrxmlSanitizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LlmManager {

    private final OpenAiCompatibleService genericService;
    private final JasperService jasperService;
    private final ReportTemplateRepository repository;

    @Value("${api.key.groq}") private String groqKey;
    @Value("${api.key.mistral}") private String mistralKey;
    @Value("${api.key.openrouter}") private String openRouterKey;

    public LlmManager(OpenAiCompatibleService genericService,
                      JasperService jasperService,
                      ReportTemplateRepository repository) {
        this.genericService = genericService;
        this.jasperService = jasperService;
        this.repository = repository;
    }

    public String generateReport(String prompt, String schema, boolean shouldSave) {
        record Attempt(String provider, String model, String url) {}

        // 1. CHECK MEMORY
        var cached = repository.findByPrompt(prompt);
        if (cached.isPresent()) {
            System.out.println(">>> [MEMORY] Found cached template for: " + prompt);
            return cached.get().getJrxmlCode();
        }

        // Build base prompt once
        String basePrompt = "DATA SCHEMA (Use these exact keys and types): " + schema
                + "\n\nUSER REQUEST: " + prompt;

        String exampleXml = repository.findAll().stream()
                .filter(ReportTemplate::isExample)
                .map(t -> "EXAMPLE STYLE [" + t.getPrompt() + "]:\n" + t.getJrxmlCode() + "\n---")
                .collect(java.util.stream.Collectors.joining("\n"));

        Attempt[] strategies = {
                new Attempt("MISTRAL",      "mistral-large-latest",          "https://api.mistral.ai/v1"),
                new Attempt("GROQ",         "llama-3.3-70b-versatile",       "https://api.groq.com/openai/v1"),
                new Attempt("OPEN_ROUTER", "minimax/minimax-m2.5", "https://openrouter.ai/api/v1"),
                new Attempt("OPEN_ROUTER", "z-ai/glm-5", "https://openrouter.ai/api/v1"),
                new Attempt("OPEN_ROUTER",  "qwen/qwen3-coder-next",         "https://openrouter.ai/api/v1"),
                new Attempt("OPEN_ROUTER",  "moonshotai/kimi-k2",            "https://openrouter.ai/api/v1"),
                new Attempt("OPEN_ROUTER",  "google/gemini-3.1-pro-preview", "https://openrouter.ai/api/v1"),
        };

        String lastError = "";

        for (Attempt attempt : strategies) {
            System.out.println(">>> Provider: " + attempt.provider() + " (" + attempt.model() + ")");

            // 🔴 FIX: currentPrompt starts as basePrompt, gets updated with errors on retry
            String currentPrompt = basePrompt;

            for (int tryCount = 1; tryCount <= 3; tryCount++) {
                try {
                    System.out.println("   |-- Attempt " + tryCount + "/3 with " + attempt.model());

                    // 🔴 FIX: Pass currentPrompt (not hardcoded finalPrompt)
                    String rawResponse = genericService.generate(
                            attempt.provider(),
                            getApiKey(attempt.provider()),
                            attempt.url(),
                            attempt.model(),
                            currentPrompt,
                            exampleXml
                    );

                    String currentXml = JrxmlSanitizer.sanitize(rawResponse);
                    String validation = jasperService.validateAndCompile(currentXml);

                    if ("VALID".equals(validation)) {
                        System.out.println("   [✓] SUCCESS with " + attempt.model());
                        if (shouldSave) {
                            repository.save(new ReportTemplate(prompt, currentXml));
                            System.out.println("   [DISK] Saved to DB.");
                        }
                        return currentXml;
                    }

                    // 🔴 FIX: Build detailed error prompt for next retry
                    System.out.println("   [✗] Validation failed: " + validation.substring(0, Math.min(120, validation.length())));
                    currentPrompt = buildFixPrompt(basePrompt, currentXml, validation);
                    lastError = validation;

                } catch (Exception e) {
                    System.err.println("   [!] Exception on attempt " + tryCount + ": " + e.getMessage());
                    lastError = e.getMessage();
                    break; // Don't retry on network/auth errors, switch provider
                }
            }
        }

        throw new RuntimeException("All providers failed. Last error: " + lastError);
    }

    private String buildFixPrompt(String originalRequest, String badXml, String error) {
        return originalRequest + "\n\n"
                + "=== YOUR PREVIOUS OUTPUT WAS INVALID. FIX IT. ===\n"
                + "ERROR: " + error + "\n\n"
                + "RULES YOU VIOLATED (DO NOT REPEAT):\n"
                + "- NEVER put forecolor or color inside a <font> tag. Put forecolor on <reportElement> instead.\n"
                + "- NEVER put <box> as a direct child of <band>. It must be INSIDE <textField> or <staticText>.\n"
                + "- NEVER let y + height exceed the band height attribute.\n"
                + "- NEVER use isSplitAllowed or splitType on bands.\n"
                + "- NEVER use isStretchWithOverflow — use textAdjust=\"StretchHeight\" instead.\n\n"
                + "BAD XML YOU GENERATED:\n" + badXml + "\n\n"
                + "OUTPUT ONLY THE CORRECTED JRXML. NO EXPLANATION. NO MARKDOWN.";
    }

    private String getApiKey(String provider) {
        return switch (provider) {
            case "GROQ" -> groqKey;
            case "MISTRAL" -> mistralKey;
            default -> openRouterKey;
        };
    }
}