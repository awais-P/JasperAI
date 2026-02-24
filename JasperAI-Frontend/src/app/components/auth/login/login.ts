import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  email: string = '';
  password: string = '';
  rememberMe: boolean = false;
  isLoading: boolean = false;

  constructor(private router: Router) {}

  onSubmit(): void {
    if (!this.email || !this.password) return;
    
    this.isLoading = true;
    // Simulate login
    setTimeout(() => {
      this.isLoading = false;
      this.router.navigate(['/dashboard']);
    }, 1000);
  }
}
