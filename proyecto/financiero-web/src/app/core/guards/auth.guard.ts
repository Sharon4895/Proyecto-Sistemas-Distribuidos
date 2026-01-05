import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

// GUARDIA 1: Protege rutas privadas (Client y Admin)
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // A. ¿Está logueado?
  if (!authService.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }

  // B. Verificación de Roles
  const userRole = authService.getUserRole();
  const requiredRole = route.data['role']; // Leemos qué rol pide la ruta

  // Si la ruta pide un rol específico y no coincide...
  if (requiredRole && userRole !== requiredRole) {
    // Si es ADMIN intentando entrar a Client -> Lo mandamos a su Admin Dashboard
    if (userRole === 'ADMIN') {
      router.navigate(['/admin/dashboard']);
    } 
    // Si es USER intentando entrar a Admin -> Lo mandamos a su Client Dashboard
    else {
      router.navigate(['/client/dashboard']);
    }
    return false;
  }

  return true; // Pase usted
};

// GUARDIA 2: Protege el Login (Si ya estás dentro, te saca de ahí)
export const publicGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    const role = authService.getUserRole();
    // Si ya está logueado, redirigir según su rol
    if (role === 'ADMIN') {
      router.navigate(['/admin/dashboard']);
    } else {
      router.navigate(['/client/dashboard']);
    }
    return false;
  }
  
  return true; // Si no está logueado, puede ver el login
};