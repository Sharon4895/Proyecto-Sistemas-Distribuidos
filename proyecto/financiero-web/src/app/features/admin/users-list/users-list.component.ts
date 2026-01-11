import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // <--- Necesario para el buscador
import { RouterModule } from '@angular/router';

// PrimeNG Imports
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DialogModule } from 'primeng/dialog'; // <--- Para el modal de detalles
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';

import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule,
    TableModule, 
    ButtonModule, 
    InputTextModule,
    DialogModule,
    TooltipModule,
    TagModule,
    RouterModule
  ],
  templateUrl: './users-list.component.html'
})
export class UsersListComponent implements OnInit {
  
  users: any[] = [];
  userTransactions: any[] = [];
  loading: boolean = true;
  
  // Variables para el Modal de Detalle
  displayDialog: boolean = false;
  selectedUser: any = null;

  constructor(private adminService: AdminService) {}

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.loading = true;
    this.adminService.getUsers().subscribe({
      next: (data) => {
        this.users = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar usuarios', err);
        this.loading = false;
      }
    });
  }

  // Abre el modal con la info del usuario
  showDetails(user: any) {
    this.selectedUser = user;
    this.displayDialog = true;
  }

  // Simulación de estado (El backend no lo manda, así que calculamos)
  getUserStatus(balance: number): string {
    return balance >= 0 ? 'ACTIVO' : 'DEUDOR';
  }

  getSeverity(status: string): "success" | "danger" | "info" | "warning" | "secondary" | "contrast" | undefined {
    return status === 'ACTIVO' ? 'success' : 'danger';
  }

  viewUserLogs(user: any) {
    this.selectedUser = user;
    this.userTransactions = []; // Limpiar anterior
    this.displayDialog = true;  // Abrir modal inmediatamente

    // Pedir los datos reales al backend
    this.adminService.getUserLogs(user.id).subscribe({
      next: (data) => {
        this.userTransactions = data;
      },
      error: (err) => console.error(err)
    });
  }
}