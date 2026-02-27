package com.awais.jasperAi.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_templates")
public class ReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Changed to CLOB for Oracle compatibility
    @Lob
    @Column(unique = true)
    private String prompt;

    // Changed to CLOB to hold massive XML files
    @Lob
    @Column(name = "jrxml_code")
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
    public void setJrxmlCode(String jrxmlCode) { this.jrxmlCode = jrxmlCode; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}