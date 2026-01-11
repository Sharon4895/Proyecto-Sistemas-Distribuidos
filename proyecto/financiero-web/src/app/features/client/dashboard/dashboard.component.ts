import { Component,OnDestroy,OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms'; // <--- Para el input del monto
import { AccountService } from '../../../core/services/account.service';
import { Transaction } from '../../../core/models/financial.models';
import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../../core/models/financial.models';

// Imports de PrimeNG
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageService } from 'primeng/api';

import { interval, Subscription } from 'rxjs';
import { ToastModule } from 'primeng/toast';      // <--- 1. IMPORTANTE


@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    FormsModule,     // <--- Agregar
    DialogModule,    // <--- Agregar
    ButtonModule,    // <--- Agregar
    InputTextModule,
    ToastModule
  ],
  providers: [MessageService],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, OnDestroy {
  private updateSubscription: Subscription | null = null;

  userName: string | null = null;
  transactions: Transaction[] = [];
  balance: number = 0;
  isLoading: boolean = true;
  isTxLoading: boolean = true;
  txError: string | null = null;
  balanceError: string | null = null;

  constructor(private authService: AuthService, private accountService: AccountService, private messageService: MessageService) {}

  ngOnInit() {
    this.loadBalance();
    const user = this.authService.getUserFromToken();
    this.userName = user?.name || null;
    this.loadTransactions();

    this.refreshData();

    this.updateSubscription = interval(7000).subscribe(() => {
      this.refreshData();
    });
  }

  refreshData() {
    this.loadBalance();
    this.loadTransactions();
  }

  ngOnDestroy() {
    if (this.updateSubscription) {
      this.updateSubscription.unsubscribe();
    }
  }

  loadTransactions() {
    this.isTxLoading = true;
    this.txError = null;
    this.accountService.getTransactions().subscribe({
      next: (data) => {
        this.transactions = data.slice(0, 5);
        this.isTxLoading = false;
      },
      error: (err) => {
        this.txError = 'No se pudo cargar el historial de movimientos.';
        this.isTxLoading = false;
        console.error('Error cargando historial', err);
      }
    });
  }

  loadBalance() {
    this.isLoading = true;
    this.balanceError = null;
    this.accountService.getBalance().subscribe({
      next: (data: any) => {
        this.balance = data.balance; 
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.balanceError = 'No se pudo cargar el saldo.';
      }
    });
  }

  isIncome(type: string): boolean {
    return type === 'DEPOSIT' || type === 'TRANSFER_RECEIVED';
  }

  // 2. Obtener el icono adecuado según el tipo
  getIcon(type: string): string {
    switch (type) {
      case 'DEPOSIT': return 'pi pi-plus-circle';           // Depósito
      case 'WITHDRAW': return 'pi pi-minus-circle';         // Retiro
      case 'TRANSFER_SENT': return 'pi pi-send';            // Envíaste dinero
      case 'TRANSFER_RECEIVED': return 'pi pi-wallet';      // Recibiste dinero
      default: return 'pi pi-circle';
    }
  }

  getColorClass(type: string): string {
    return this.isIncome(type) ? 'text-green-500 bg-green-100' : 'text-red-500 bg-red-100';
  }

  // Variables para controlar la ventana emergente
  displayModal: boolean = false;
  transactionType: 'DEPOSIT' | 'WITHDRAW' = 'DEPOSIT';
  amount: number | null = null;

  // Abrir el modal según el tipo de operación
  openModal(type: 'DEPOSIT' | 'WITHDRAW') {
    this.transactionType = type;
    this.amount = null; // Reiniciar monto
    this.displayModal = true;
  }

  confirmTransaction() {
    if (this.amount && this.amount > 0) {
      
      const type = this.transactionType === 'DEPOSIT' ? 'DEPOSIT' : 'WITHDRAW';
      
      // Usar el servicio
      this.accountService.operate(type, this.amount).subscribe({
        
        // 1. Caso de ÉXITO (Next)
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Operación Exitosa',
            detail: `Has realizado un ${this.transactionType === 'DEPOSIT' ? 'depósito' : 'retiro'} de $${this.amount}`,
            life: 3000
          });
          
          this.displayModal = false; // Cerramos el modal
          this.amount = null;        // Limpiamos el campo
          this.loadBalance();        // Recargamos el saldo nuevo
        },

        // 2. Caso de ERROR (Aquí entra cuando throwError se activa en el servicio)
        error: (err) => {
          this.messageService.add({
            severity: 'error',       // Rojo
            summary: 'Operación Rechazada',
            detail: err.message || 'No tienes fondos suficientes para realizar este retiro.', // Mensaje para el usuario
            life: 3000
          });
          // Nota: NO cerramos el modal (displayModal = false) para que el usuario pueda corregir el monto.
        }
      });

    } else {
        // Validación local (Monto 0 o negativo)
        this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'El monto debe ser mayor a 0',
            life: 3000
        });
    }
  }
  
  // Getter para el título dinámico
  get modalTitle(): string {
    return this.transactionType === 'DEPOSIT' ? 'Realizar Depósito' : 'Realizar Retiro';
  }
}