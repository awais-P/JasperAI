import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../shared/services/auth-service';
import { ToastService } from '../../../shared/services/toast-service';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  loginForm!: FormGroup;
  errorMessage: string = '';
  isLoading: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private toastService: ToastService
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      rememberMe: [false]
    });
  }
  onSubmit() {
    if (this.loginForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.toastService.success('Welcome back! Redirecting to dashboard...');
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;
        const message = this.getErrorMessage(err);
        this.errorMessage = message;
        this.toastService.error(message);
      }
    });
  }

  private getErrorMessage(err: any): string {
    if (err.status === 403) {
      return 'Invalid email or password. Please try again.';
    }
    if (err.status === 401) {
      return 'Invalid credentials. Please check your email and password.';
    }
    if (err.status === 0) {
      return 'Unable to connect to server. Please try again later.';
    }
    return err.error?.error || err.error?.message || 'Login failed. Please try again.';
  }
}
