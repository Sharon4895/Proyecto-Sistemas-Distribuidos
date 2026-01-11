import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

// GUARDIA 1: Protege rutas privadas (Client y Admin)
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Solo verifica si hay token
  if (!authService.isAuthenticated()) {
    return router.parseUrl('/login');
  }
  return true;
};

// GUARDIA 2: Protege el Login (Si ya estás dentro, te saca de ahí)
export const publicGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    const user = authService.getUserFromToken();
    if (user?.role === 'ADMIN') {
      return router.parseUrl('/admin/dashboard');
    } else {
      return router.parseUrl('/client/dashboard');
    }
  }
  return true;
};