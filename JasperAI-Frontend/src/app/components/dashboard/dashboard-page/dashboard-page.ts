import { Component, ElementRef, HostListener, ViewChild } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ReportService } from '../../../shared/services/report-service';

@Component({
  selector: 'app-dashboard-page',
  standalone: false,
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage {
@ViewChild('canvasRef') canvasRef!: ElementRef;
  
promptText: string = '';
  isGenerating: boolean = false;
  errorMessage: string = '';
  pdfUrl: SafeResourceUrl | null = null;
  rawPdfBlob: Blob | null = null;

  // Visual Editor Configuration
  reportConfig = {
    page: { format: 'A4', width: 595, height: 842 },
    zoom: 100,
    margins: { top: 40, bottom: 40, left: 40, right: 40 },
    bands: { title: 60, pageHeader: 40, columnHeader: 30, detail: 40, pageFooter: 40, summary: 50 },
    columns: [
      { name: 'ID', width: 20 },
      { name: 'Product Name', width: 50 },
      { name: 'Price', width: 30 }
    ],
    elements: [
      // Example default element
      { id: 1, type: 'static', text: 'Invoice Report', x: 200, y: 20, style: { fontSize: 24, bold: true, italic: false, underline: false } }
    ]
  };

  previousTemplates = [
    { id: 1, name: 'Sales Report Q4 2025', date: 'Feb 20, 2026', status: 'completed' },
    { id: 2, name: 'Inventory Summary', date: 'Feb 18, 2026', status: 'completed' }
  ];

  // The actual data
  tableData = [
    { invoiceNumber: "INV-001", totalAmount: 500.0, customerName: "John Doe" },
    { invoiceNumber: "INV-002", totalAmount: 150.0, customerName: "Jane Smith" }
  ];

  jsonData: string = '';
  selectedIds: Set<number> = new Set();
  dragState = { 
    isDragging: false, targetType: '', id: null as any, 
    startX: 0, startY: 0, startValX: 0, startValY: 0 
  };
  marquee = { active: false, startX: 0, startY: 0, currentX: 0, currentY: 0 };

  constructor(
    private reportService: ReportService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    this.updateJsonTextarea();
  }

  // --- KEYBOARD CONTROLS (DELETE ELEMENTS) ---
  @HostListener('window:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent) {
    // Only delete if we aren't typing inside an actual input box
    if ((event.key === 'Delete' || event.key === 'Backspace') && 
        !(event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement)) {
      
      if (this.selectedIds.size > 0) {
        this.reportConfig.elements = this.reportConfig.elements.filter(el => !this.selectedIds.has(el.id));
        this.selectedIds.clear();
        this.updateJsonTextarea();
      }
    }
  }

  // --- ADDING ELEMENTS ---
  addStaticText() {
    const newId = Date.now();
    this.reportConfig.elements.push({
      id: newId, type: 'static', text: 'New Text', 
      x: this.reportConfig.margins.left + 10, y: this.reportConfig.margins.top + 10,
      style: { fontSize: 12, bold: false, italic: false, underline: false }
    });
    this.selectElement(newId, false);
    this.updateJsonTextarea();
  }

  addVariablePair() {
    const labelId = Date.now();
    const varId = labelId + 1;
    const spawnX = this.reportConfig.margins.left + 10;
    const spawnY = this.reportConfig.margins.top + 50;

    // Static Label ("Name: ")
    this.reportConfig.elements.push({
      id: labelId, type: 'static', text: 'Field Name:', 
      x: spawnX, y: spawnY,
      style: { fontSize: 12, bold: true, italic: false, underline: false }
    });

    // Dynamic Line Variable
    this.reportConfig.elements.push({
      id: varId, type: 'variable', text: '$F{dynamic_field}', 
      x: spawnX + 80, y: spawnY,
      style: { fontSize: 12, bold: false, italic: false, underline: true } // Underline acts as the blank line
    });

    this.selectedIds.clear();
    this.selectedIds.add(labelId);
    this.selectedIds.add(varId);
    this.updateJsonTextarea();
  }

  // --- SELECTION LOGIC ---
  selectElement(id: number, ctrlKey: boolean) {
    if (ctrlKey) {
      if (this.selectedIds.has(id)) this.selectedIds.delete(id);
      else this.selectedIds.add(id);
    } else {
      if (!this.selectedIds.has(id)) {
        this.selectedIds.clear();
        this.selectedIds.add(id);
      }
    }
  }

  clearSelection() {
    this.selectedIds.clear();
  }

  // --- TEXT FORMATTING TOOLBAR ---
  applyStyle(property: string, value?: any) {
    this.reportConfig.elements.forEach(el => {
      if (this.selectedIds.has(el.id)) {
        if (property === 'bold') el.style.bold = !el.style.bold;
        if (property === 'italic') el.style.italic = !el.style.italic;
        if (property === 'underline') el.style.underline = !el.style.underline;
        if (property === 'sizeUp') el.style.fontSize += 2;
        if (property === 'sizeDown' && el.style.fontSize > 6) el.style.fontSize -= 2;
      }
    });
    this.updateJsonTextarea();
  }

  // --- DRAG, DROP, & MARQUEE LOGIC ---
  onCanvasMouseDown(event: MouseEvent) {
    // If clicking directly on the canvas background, start marquee selection
    if ((event.target as HTMLElement).classList.contains('jasper-canvas')) {
      this.clearSelection();
      const rect = this.canvasRef.nativeElement.getBoundingClientRect();
      const scale = this.reportConfig.zoom / 100;
      
      const x = (event.clientX - rect.left) / scale;
      const y = (event.clientY - rect.top) / scale;

      this.marquee = { active: true, startX: x, startY: y, currentX: x, currentY: y };
      event.preventDefault();
    }
  }

  selectElementsInMarquee() {
    const xMin = Math.min(this.marquee.startX, this.marquee.currentX);
    const xMax = Math.max(this.marquee.startX, this.marquee.currentX);
    const yMin = Math.min(this.marquee.startY, this.marquee.currentY);
    const yMax = Math.max(this.marquee.startY, this.marquee.currentY);

    this.selectedIds.clear();
    this.reportConfig.elements.forEach(el => {
      // Simple point-in-box collision detection
      if (el.x >= xMin && el.x <= xMax && el.y >= yMin && el.y <= yMax) {
        this.selectedIds.add(el.id);
      }
    });
  }

  // --- TOOLBAR SETTINGS ---
  changePageSize(event: any) {
    const size = event.target.value;
    if (size === 'A4-P') this.reportConfig.page = { format: 'A4', width: 595, height: 842 };
    if (size === 'A4-L') this.reportConfig.page = { format: 'A4', width: 842, height: 595 };
    if (size === 'A3-P') this.reportConfig.page = { format: 'A3', width: 842, height: 1191 };
    this.updateJsonTextarea();
  }

 zoomIn() {
    if (this.reportConfig.zoom < 200) this.reportConfig.zoom += 10;
    this.updateJsonTextarea();
  }

  zoomOut() {
    if (this.reportConfig.zoom > 40) this.reportConfig.zoom -= 10;
    this.updateJsonTextarea();
  }

  addColumn() {
    this.reportConfig.columns.push({ name: 'New Col', width: 20 });
    this.updateJsonTextarea();
  }

  removeColumn(index: number) {
    this.reportConfig.columns.splice(index, 1);
    this.updateJsonTextarea();
  }

  addTextElement() {
    const newId = this.reportConfig.elements.length 
      ? Math.max(...this.reportConfig.elements.map((el: any) => el.id)) + 1 
      : 1;
    this.reportConfig.elements.push({
      id: newId,
      type: 'variable',
      text: '$F{new_field}',
      x: this.reportConfig.margins.left + 20,
      y: this.reportConfig.margins.top + 20,
      style: { fontSize: 12, bold: false, italic: false, underline: false }
    });
    this.selectElement(newId, false);
    this.updateJsonTextarea();
  }

  // --- UNIFIED DRAG & DROP ENGINE ---
  startDrag(targetType: 'band' | 'margin' | 'element', id: any, event: MouseEvent) {
    event.stopPropagation(); // Prevent triggering canvas marquee
    
    if (targetType === 'element') {
      // If we drag an element that isn't selected, select it first
      if (!this.selectedIds.has(id)) this.selectElement(id, event.ctrlKey);
    }

    let startValX = 0; let startValY = 0;

    if (targetType === 'band') startValY = (this.reportConfig.bands as any)[id];
    else if (targetType === 'margin') {
      startValY = (id === 'top' || id === 'bottom') ? (this.reportConfig.margins as any)[id] : 0;
      startValX = (id === 'left' || id === 'right') ? (this.reportConfig.margins as any)[id] : 0;
    }

    this.dragState = {
      isDragging: true, targetType, id,
      startX: event.clientX, startY: event.clientY,
      startValX, startValY
    };
  }

 @HostListener('window:mousemove', ['$event'])
  onMouseMove(event: MouseEvent) {
    const scale = this.reportConfig.zoom / 100;

    // Handle Marquee Dragging
    if (this.marquee.active) {
      const rect = this.canvasRef.nativeElement.getBoundingClientRect();
      this.marquee.currentX = (event.clientX - rect.left) / scale;
      this.marquee.currentY = (event.clientY - rect.top) / scale;
      this.selectElementsInMarquee();
      return;
    }

    // Handle Normal Dragging
    if (!this.dragState.isDragging) return;
    
    const deltaX = (event.clientX - this.dragState.startX) / scale;
    const deltaY = (event.clientY - this.dragState.startY) / scale;

    if (this.dragState.targetType === 'band') {
      (this.reportConfig.bands as any)[this.dragState.id] = Math.max(20, this.dragState.startValY + deltaY);

    } else if (this.dragState.targetType === 'margin') {
      const dir = this.dragState.id;
      if (dir === 'top') this.reportConfig.margins.top = Math.max(0, this.dragState.startValY + deltaY);
      if (dir === 'bottom') this.reportConfig.margins.bottom = Math.max(0, this.dragState.startValY - deltaY);
      if (dir === 'left') this.reportConfig.margins.left = Math.max(0, this.dragState.startValX + deltaX);
      if (dir === 'right') this.reportConfig.margins.right = Math.max(0, this.dragState.startValX - deltaX);
      
    } else if (this.dragState.targetType === 'element') {
      // Move ALL currently selected elements
      this.reportConfig.elements.forEach(el => {
        if (this.selectedIds.has(el.id)) {
          // Initialize drag origins on the fly if not set
          if (!(el as any)._dragStartX) {
            (el as any)._dragStartX = el.x;
            (el as any)._dragStartY = el.y;
          }
          el.x = (el as any)._dragStartX + deltaX;
          el.y = (el as any)._dragStartY + deltaY;
        }
      });
    }
    
    this.updateJsonTextarea();
  }

  @HostListener('window:mouseup')
  onMouseUp() {
    this.dragState.isDragging = false;
    this.marquee.active = false;
    
    // Clear temporary drag origins
    this.reportConfig.elements.forEach(el => {
      delete (el as any)._dragStartX;
      delete (el as any)._dragStartY;
    });
  }

  // --- DATA SYNC & API ---
  updateJsonTextarea() {
    this.jsonData = JSON.stringify(this.reportConfig, null, 2);
  }

  onJsonManualEdit() {
    try {
      this.reportConfig = JSON.parse(this.jsonData);
      this.errorMessage = '';
    } catch(e) {}
  }
  // --- REPORT GENERATION ---
  generateReport(): void {
    if (!this.promptText.trim()) {
      this.errorMessage = "Please provide an AI prompt.";
      return;
    }

    this.errorMessage = '';
    this.isGenerating = true;
    this.pdfUrl = null;
    this.rawPdfBlob = null;

    const apiPayload = {
      prompt: this.promptText,
      config: this.reportConfig
    };

    this.reportService.generatePdf(apiPayload).subscribe({
      next: async (res: any) => {
        this.isGenerating = false;
        let finalBlob = res;

        // THE FIX: Catch the [37, 80, 68...] byte array and convert to PDF
        if (res.type === 'application/json' || !res.type) {
          try {
            const text = await res.text();
            const byteArray = JSON.parse(text); 
            const uint8Array = new Uint8Array(byteArray);
            finalBlob = new Blob([uint8Array], { type: 'application/pdf' });
          } catch(e) {
            // Fallback if it's already a blob
          }
        }

        this.rawPdfBlob = finalBlob;
        const objectUrl = URL.createObjectURL(finalBlob);
        this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl);
      },
      error: (err) => {
        this.isGenerating = false;
        this.errorMessage = "Failed to generate report. Check console.";
        console.error(err);
      }
    });
  }

  downloadPdf(): void {
    if (!this.rawPdfBlob) return;
    const url = window.URL.createObjectURL(this.rawPdfBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `JasperAI_Report.pdf`;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  clearPrompt(): void {
    this.promptText = '';
  }

  loadTemplate(template: any): void {
    // TODO: Load the selected template configuration
    console.log('Loading template:', template.name);
  }

// Helper for Marquee Box CSS
  get marqueeStyle() {
    return {
      left: Math.min(this.marquee.startX, this.marquee.currentX) + 'px',
      top: Math.min(this.marquee.startY, this.marquee.currentY) + 'px',
      width: Math.abs(this.marquee.startX - this.marquee.currentX) + 'px',
      height: Math.abs(this.marquee.startY - this.marquee.currentY) + 'px'
    };
  }
}

