import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { DashboardComponent } from './features/client/dashboard/dashboard.component';
import { TransactionsComponent } from './features/client/transactions/transactions.component';
import { TransferComponent } from './features/client/transfer/transfer.component';
import { AdminDashboardComponent } from './features/admin/admin-dashboard/admin-dashboard.component';
import { UsersListComponent } from './features/admin/users-list/users-list.component';

// IMPORTAMOS LOS GUARDIAS CREADOS
import { authGuard, publicGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // Ruta por defecto
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // --- RUTAS PÚBLICAS (Login/Register) ---
  // Usamos 'publicGuard' para que si ya tienen sesión, los bote al dashboard
  { 
    path: 'login', 
    component: LoginComponent, 
    canActivate: [publicGuard] 
  },
  { 
    path: 'register', 
    component: RegisterComponent, 
    canActivate: [publicGuard] 
  },

  // --- ÁREA DE CLIENTE (Solo Rol USER) ---
  { 
    path: 'client',
    canActivate: [authGuard],      // <--- Protección activada
    data: { role: 'USER' },        // <--- Requisito: Ser USER
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'transactions', component: TransactionsComponent },
      { path: 'transfer', component: TransferComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  // --- ÁREA DE ADMINISTRADOR (Solo Rol ADMIN) ---
  { 
    path: 'admin',
    canActivate: [authGuard],      // <--- Protección activada
    data: { role: 'ADMIN' },       // <--- Requisito: Ser ADMIN
    children: [
      { path: 'dashboard', component: AdminDashboardComponent },
      { path: 'users', component: UsersListComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  // Cualquier ruta desconocida va al login
  { path: '**', redirectTo: 'login' }
];