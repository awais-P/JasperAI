import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../shared/services/auth-service';
import { ToastService } from '../../../shared/services/toast-service';

@Component({
  selector: 'app-signup',
  standalone: false,
  templateUrl: './signup.html',
  styleUrl: './signup.scss',
})
export class Signup {
  isLoading: boolean = false;
  signupForm: FormGroup;
  errorMessage: string = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private toastService: ToastService
  ) {
    this.signupForm = this.fb.group({
      fullName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required],
      agreeToTerms: [false, Validators.requiredTrue]
    });
  }

  onSubmit() {
    // Basic password match check
    if (this.signupForm.value.password !== this.signupForm.value.confirmPassword) {
      this.errorMessage = "Passwords do not match.";
      this.toastService.error(this.errorMessage);
      return;
    }

    if (this.signupForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';

    // We don't need to send confirmPassword or agreeToTerms to the backend
    const { confirmPassword, agreeToTerms, ...apiPayload } = this.signupForm.value;

    this.authService.signup(apiPayload).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.toastService.success('Account created successfully! Welcome to JasperAI.');
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
    if (err.status === 409) {
      return 'An account with this email already exists.';
    }
    if (err.status === 0) {
      return 'Unable to connect to server. Please try again later.';
    }
    return err.error?.error || err.error?.message || 'Signup failed. Please try again.';
  }
}