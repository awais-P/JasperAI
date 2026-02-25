import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';

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
    return this.http.post<any>(`${this.apiUrl}/login`, credentials).pipe(
      // 'tap' lets us do something with the response before it reaches the component
      tap(response => {
        if (response && response.token) {
          // Save the token and user details to LocalStorage
          localStorage.setItem('jwtToken', response.token);
          localStorage.setItem('jasperUser', JSON.stringify({
            email: response.email,
            fullName: response.fullName,
            planType: response.planType
          }));
        }
      })
    );
  }

  // Helper method to store user data locally after login
  saveUserToStorage(user: any) {
    localStorage.setItem('jasperUser', JSON.stringify(user));
  }

  getUserFromStorage() {
    const user = localStorage.getItem('jasperUser');
    return user ? JSON.parse(user) : null;
  }


  getToken(): string | null {
    return localStorage.getItem('jwtToken');
  }

  isLoggedIn(): boolean {
    return !!this.getToken(); // Returns true if token exists
  }

  logout() {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('jasperUser');
  }
}