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

    // UPDATED SIGNATURE: accepts 'shouldSave'
    public String generateReport(String prompt,String schema, boolean shouldSave) {
        record Attempt(String provider, String model, String url) {}

        // 1. CHECK MEMORY (Always return cached if it exists)
        var cached = repository.findByPrompt(prompt);
        if (cached.isPresent()) {
            System.out.println(">>> [MEMORY] Found cached template for: " + prompt);
            return cached.get().getJrxmlCode();
        }
        String finalPrompt = "DATA SCHEMA (Use these exact keys): " + schema + "\n\nUSER REQUEST: " + prompt;
        String exampleXml = repository.findAll().stream()
                .filter(ReportTemplate::isExample)
                .map(t -> "TEMPLATE STYLE: " + t.getPrompt() + "\n" + t.getJrxmlCode() + "\n---")
                .collect(java.util.stream.Collectors.joining("\n"));

        // --- STRATEGY LIST ---
        Attempt[] strategies = {
                new Attempt("MISTRAL", "mistral-large-latest", "https://api.mistral.ai/v1"),

                new Attempt("OPEN_ROUTER", "anthropic/claude-4.6-opus", "https://openrouter.ai/api/v1"),

                new Attempt("OPEN_ROUTER", "qwen/qwen3-coder-next", "https://openrouter.ai/api/v1"),

                new Attempt("OPEN_ROUTER", "qwen/qwen3-max-thinking", "https://openrouter.ai/api/v1"),

                new Attempt("GROQ", "llama-3.3-70b-versatile", "https://api.groq.com/openai/v1"),

                new Attempt("OPEN_ROUTER", "google/gemini-3.1-pro-preview", "https://openrouter.ai/api/v1")
        };

        String lastError = "";

        // --- PROVIDER LOOP ---
        for (Attempt attempt : strategies) {
            System.out.println(">>> Switching Provider: " + attempt.provider + " (" + attempt.model + ")");

            String currentXml = "";
            String currentPrompt = prompt;

            for (int tryCount = 1; tryCount <= 3; tryCount++) {
                try {
                    System.out.println("   |-- Attempt " + tryCount + "/3 with " + attempt.model);

                    // 1. Generate
                    String rawResponse = genericService.generate(attempt.provider,
                            getErrorKey(attempt.provider),
                            attempt.url, attempt.model, finalPrompt, exampleXml);

                    // 2. Sanitize
                    currentXml = JrxmlSanitizer.sanitize(rawResponse);

                    // 3. Validate
                    String validation = jasperService.validateAndCompile(currentXml);

                    if ("VALID".equals(validation)) {
                        System.out.println("   [V] SUCCESS with " + attempt.model);

                        // 4. SAVE ONLY IF REQUESTED
                        if (shouldSave) {
                            ReportTemplate newTemplate = new ReportTemplate(prompt, currentXml);
                            repository.save(newTemplate);
                            System.out.println("   [DISK] Saved template to DB.");
                        }

                        return currentXml;
                    }

                    // 5. If Invalid -> Prepare Fix Prompt
                    System.out.println("   [x] Failed Validation: " + validation.split(";")[0]);
                    currentPrompt = "You generated invalid JRXML. \n" +
                            "ERROR: " + validation + "\n" +
                            "BAD CODE: \n" + currentXml + "\n" +
                            "FIX IT. Ensure <band> has no 'backcolor' and root has no 'uuid'.";

                    lastError = validation;

                } catch (Exception e) {
                    System.err.println("   [!] Crash on attempt " + tryCount + ": " + e.getMessage());
                    lastError = e.getMessage();
                    break;
                }
            }
        }

        throw new RuntimeException("All Providers Failed. Last Error: " + lastError);
    }

    private String getErrorKey(String provider) {
        if ("GROQ".equals(provider)) return groqKey;
        if ("MISTRAL".equals(provider)) return mistralKey;
        return openRouterKey;
    }
}