import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../shared/services/auth-service';
import { ToastService } from '../../../shared/services/toast-service';

@Component({
  selector: 'app-profile',
  standalone: false,
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile implements OnInit {
  user: any = null;
  isEditing: boolean = false;
  editForm = {
    fullName: '',
    email: ''
  };

  constructor(
    private authService: AuthService,
    private router: Router,
    private toastService: ToastService
  ) {}

  ngOnInit() {
    this.user = this.authService.getUserFromStorage();
    if (this.user) {
      this.editForm.fullName = this.user.fullName || '';
      this.editForm.email = this.user.email || '';
    }
  }

  get userInitial(): string {
    if (this.user?.fullName) {
      return this.user.fullName.charAt(0).toUpperCase();
    }
    if (this.user?.email) {
      return this.user.email.charAt(0).toUpperCase();
    }
    return 'U';
  }

  get memberSince(): string {
    // For now, return a placeholder date
    return 'February 2026';
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
    if (!this.isEditing && this.user) {
      this.editForm.fullName = this.user.fullName || '';
      this.editForm.email = this.user.email || '';
    }
  }

  saveProfile() {
    // Update local storage with new values
    if (this.user) {
      this.user.fullName = this.editForm.fullName;
      this.user.email = this.editForm.email;
      this.authService.saveUserToStorage(this.user);
      this.toastService.success('Profile updated successfully!');
      this.isEditing = false;
    }
  }

  logout() {
    this.authService.logout();
    this.toastService.success('You have been logged out successfully.');
    this.router.navigate(['/']);
  }
}
