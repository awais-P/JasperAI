package com.awais.jasperAi.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_templates")
public class ReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", unique = true) // The User's Prompt (e.g., "Green Invoice")
    private String prompt;

    @Column(columnDefinition = "TEXT") // The AI generated JRXML
    private String jrxmlCode;

    private LocalDateTime createdAt;
    private boolean isExample;

    // Getters, Setters, Constructors
    public ReportTemplate() {}

    public ReportTemplate(String prompt, String jrxmlCode) {
        this.prompt = prompt;
        this.jrxmlCode = jrxmlCode;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExample() { return isExample; }
    public void setExample(boolean example) { isExample = example; }
    public String getJrxmlCode() { return jrxmlCode; }
    public String getPrompt() { return prompt; }
}