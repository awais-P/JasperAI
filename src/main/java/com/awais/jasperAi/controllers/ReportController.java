package com.awais.jasperAi.controllers;

import com.awais.jasperAi.dto.ReportRequest;
import com.awais.jasperAi.entities.ReportTemplate;
import com.awais.jasperAi.repositories.ReportTemplateRepository; // IMPORT THIS
import com.awais.jasperAi.services.JasperService;
import com.awais.jasperAi.services.LlmManager;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final LlmManager llmManager;
    private final JasperService jasperService;
    private final ReportTemplateRepository repository; // 1. Inject Repository

    // 2. Add Repository to Constructor
    public ReportController(LlmManager llmManager,
                            JasperService jasperService,
                            ReportTemplateRepository repository) {
        this.llmManager = llmManager;
        this.jasperService = jasperService;
        this.repository = repository;
    }

    @PostMapping("/generate")
    public String generateReport(@RequestBody String prompt) {
        // Default to FALSE (don't save) for quick previews
        return llmManager.generateReport(prompt, false);
    }

    @PostMapping(value = "/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public byte[] exportReport(@RequestBody ReportRequest request) throws Exception {
        // Pass the user's specific 'save' flag
        String xml = llmManager.generateReport(request.prompt(),request.shouldSave());

        if (!xml.trim().startsWith("<?xml")) {
            throw new RuntimeException("AI failed to generate valid XML.");
        }

        return jasperService.exportToPdf(xml, new ArrayList<>(request.data()));
    }

    @PostMapping("/teach")
    public String teachAi(@RequestBody Map<String, String> payload) {
        String prompt = payload.get("styleName");
        String xml = payload.get("xml");

        ReportTemplate template = new ReportTemplate(prompt, xml);
        template.setExample(true); // Mark as Golden Example
        repository.save(template); // Now 'repository' is recognized

        return "AI has learned style: " + prompt;
    }
}