import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

// GUARDIA: Solo permite acceso a ADMIN
export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const user = authService.getUserFromToken();

  if (!authService.isAuthenticated() || user?.role !== 'ADMIN') {
    router.navigate(['/login']);
    return false;
  }
  return true;
};
