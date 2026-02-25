import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth'; 

  constructor(private http: HttpClient) { }

  signup(userData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/signup`, userData);
  }

  login(credentials: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credentials);
  }

  // Helper method to store user data locally after login
  saveUserToStorage(user: any) {
    localStorage.setItem('jasperUser', JSON.stringify(user));
  }

  getUserFromStorage() {
    const user = localStorage.getItem('jasperUser');
    return user ? JSON.parse(user) : null;
  }

  logout() {
    localStorage.removeItem('jasperUser');
  }
}