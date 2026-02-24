package com.awais.jasperAi.services;

import net.sf.jasperreports.engine.*;
import org.springframework.stereotype.Service;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

@Service
public class JasperService {
    public String validateAndCompile(String xmlContent) {
        try {
            // Convert String to Stream
            ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8));

            // THE MAGIC: Try to compile it.
            // If this throws an exception, the XML is bad.
            JasperReport report = JasperCompileManager.compileReport(inputStream);

            return "VALID";
        } catch (JRException e) {
            // If it fails, return the error message so we can tell Gemini (later)
            return "ERROR: " + e.getMessage();
        }
    }

    // Change the parameter from Object to ?
    public byte[] exportToPdf(String jrxml, Collection<Map<String, ?>> data) throws JRException {
        // 1. Load and Compile the JRXML
        JasperReport jasperReport = JasperCompileManager.compileReport(
                new ByteArrayInputStream(jrxml.getBytes())
        );

        // 2. Create the Data Source (Now the types match perfectly!)
        JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(data);

        // 3. Fill the report
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, null, dataSource);

        // 4. Export to PDF bytes
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

}
