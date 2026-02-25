import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth-service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    // 1. Check if the user has a valid token
    if (this.authService.isLoggedIn()) {
      return true; // Let them pass!
    }

    // 2. If no token, redirect them to the login page
    this.router.navigate(['/auth/login'], { 
      // Optional: Save the URL they were trying to access so you can redirect them back after a successful login
      queryParams: { returnUrl: state.url } 
    });
    
    return false; // Block the navigation
  }
}