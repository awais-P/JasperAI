import { Component } from '@angular/core';

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

  previousTemplates = [
    { id: 1, name: 'Sales Report Q4 2025', date: 'Feb 20, 2026', status: 'completed' },
    { id: 2, name: 'Inventory Summary', date: 'Feb 18, 2026', status: 'completed' },
    { id: 3, name: 'Employee Performance', date: 'Feb 15, 2026', status: 'completed' },
    { id: 4, name: 'Financial Statement', date: 'Feb 10, 2026', status: 'completed' },
  ];

  generateReport(): void {
    if (!this.promptText.trim()) return;
    
    this.isGenerating = true;
    // Simulating report generation
    setTimeout(() => {
      this.outputResult = `Generated Report Preview\n\nBased on your prompt: "${this.promptText}"\n\nReport data will appear here once the backend integration is complete.`;
      this.isGenerating = false;
    }, 1500);
  }

  clearPrompt(): void {
    this.promptText = '';
    this.outputResult = '';
  }

  loadTemplate(template: any): void {
    this.promptText = `Load template: ${template.name}`;
  }
}
