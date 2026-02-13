package com.awais.jasperAi.repositories;

import com.awais.jasperAi.entities.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {
    // This magic method finds a report by its prompt
    Optional<ReportTemplate> findByPrompt(String prompt);
    Optional<ReportTemplate> findFirstByIsExampleTrue();
}