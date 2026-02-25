import { Component } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ReportService } from '../../../shared/services/report-service';

@Component({
  selector: 'app-dashboard-page',
  standalone: false,
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage {
  promptText: string = '';
  outputResult: string = '';
  isGenerating: boolean = false;
  errorMessage: string = '';
  jsonData: string = '[\n  {\n    "invoiceNumber": "INV-001",\n    "totalAmount": 500.0,\n    "customerName": "John Doe"\n  }\n]';
  pdfUrl: SafeResourceUrl | null = null;
  rawPdfBlob: Blob | null = null;

  previousTemplates = [
    { id: 1, name: 'Sales Report Q4 2025', date: 'Feb 20, 2026', status: 'completed' },
    { id: 2, name: 'Inventory Summary', date: 'Feb 18, 2026', status: 'completed' }
  ];

  constructor(
    private reportService: ReportService,
    private sanitizer: DomSanitizer // Needed to safely render the PDF blob
  ) {}

  generateReport(): void {
    if (!this.promptText.trim() || !this.jsonData.trim()) {
      this.errorMessage = "Please provide both a prompt and JSON data.";
      return;
    }

    this.errorMessage = '';
    this.isGenerating = true;
    this.pdfUrl = null;
    this.rawPdfBlob = null;

    let parsedData = [];
    try {
      parsedData = JSON.parse(this.jsonData);
      if (!Array.isArray(parsedData)) {
        throw new Error("JSON must be an array of objects.");
      }
    } catch (e) {
      this.errorMessage = "Invalid JSON format. Please check your data.";
      this.isGenerating = false;
      return;
    }

    const payload = {
      prompt: this.promptText,
      data: parsedData,
      save: true // Assuming we want to save this to the DB cache
    };

    this.reportService.generatePdf(payload).subscribe({
      next: (blob: Blob) => {
        this.isGenerating = false;
        this.rawPdfBlob = blob;
        
        // Convert the raw PDF bytes into a URL the browser can display
        const objectUrl = URL.createObjectURL(blob);
        this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl);
      },
      error: (err) => {
        this.isGenerating = false;
        console.error("Report generation failed", err);
        this.errorMessage = "The AI failed to generate the report. Please try modifying your prompt.";
      }
    });
  }

  downloadPdf(): void {
    if (!this.rawPdfBlob) return;
    
    const url = window.URL.createObjectURL(this.rawPdfBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `Report_${new Date().getTime()}.pdf`;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  clearPrompt(): void {
    this.promptText = '';
    this.pdfUrl = null;
    this.errorMessage = '';
  }

  loadTemplate(template: any): void {
    this.promptText = `Load template: ${template.name}`;
  }
}
