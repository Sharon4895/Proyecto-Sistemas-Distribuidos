import { Component, OnInit } from '@angular/core'; // <--- Importar OnInit
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
// ... imports de PrimeNG ...
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { AccountService } from '../../../core/services/account.service'; // <--- Importar Servicio
import { Transaction } from '../../../core/models/financial.models';   // <--- Importar Interfaz

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, RouterModule, TableModule, TagModule, ButtonModule, InputTextModule],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.scss'
})
export class TransactionsComponent implements OnInit {
  
  transactions: Transaction[] = []; // Array vacío al inicio
  loading: boolean = true;          // Para mostrar carga en la tabla

  constructor(private accountService: AccountService) {} // <--- Inyectar

  ngOnInit() {
    this.loading = true;
    this.accountService.getTransactions().subscribe({
      next: (data) => {
        this.transactions = data; // Aquí cargamos TODAS
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        console.error(err);
      }
    });
  }

  // ... la función getSeverity se queda igual ...
  getSeverity(type: string) { 
      // ... (mismo código de antes)
      switch (type) {
        case 'DEPOSIT': return 'success';
        case 'TRANSFER_RECEIVED': return 'success';
        case 'WITHDRAWAL': return 'warning';
        case 'TRANSFER_SENT': return 'info';
        default: return 'secondary';
    }
  }
}