import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ReportService {
  private apiUrl = 'http://localhost:8080/api/reports';

  constructor(private http: HttpClient) { }

  // Notice the responseType: 'blob'. This is critical for downloading PDFs!
  generatePdf(payload: any): Observable<Blob> {
    return this.http.post(`${this.apiUrl}/export`, payload, {
      responseType: 'blob' 
    });
  }
}