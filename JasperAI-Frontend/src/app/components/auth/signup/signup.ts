import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-signup',
  standalone: false,
  templateUrl: './signup.html',
  styleUrl: './signup.scss',
})
export class Signup {
  fullName: string = '';
  email: string = '';
  password: string = '';
  confirmPassword: string = '';
  agreeToTerms: boolean = false;
  isLoading: boolean = false;

  constructor(private router: Router) {}

  onSubmit(): void {
    if (!this.fullName || !this.email || !this.password || !this.confirmPassword) return;
    if (this.password !== this.confirmPassword) return;
    if (!this.agreeToTerms) return;
    
    this.isLoading = true;
    // Simulate signup
    setTimeout(() => {
      this.isLoading = false;
      this.router.navigate(['/dashboard']);
    }, 1000);
  }
}
