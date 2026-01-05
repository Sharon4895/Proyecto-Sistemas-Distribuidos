import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms'; // <--- Necesario para formularios
import { RouterModule, Router } from '@angular/router';

// PrimeNG
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber'; // Para el dinero
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';

// Servicio
import { AccountService } from '../../../core/services/account.service';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    RouterModule, 
    CardModule, 
    InputTextModule, 
    ButtonModule, 
    InputNumberModule,
    ToastModule
  ],
  providers: [MessageService],
  templateUrl: './transfer.component.html',
  styleUrl: './transfer.component.scss'
})
export class TransferComponent {

  transferForm: FormGroup;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private accountService: AccountService,
    private messageService: MessageService,
    private router: Router
  ) {
    // Validaciones del formulario
    this.transferForm = this.fb.group({
      targetCurp: ['', [Validators.required, Validators.minLength(1)]], // CURP destino
      amount: [null, [Validators.required, Validators.min(1)]],
      description: ['', [Validators.maxLength(50)]]
    });
  }

  onTransfer() {
    if (this.transferForm.valid) {
      this.loading = true;
      const { targetCurp, amount , description } = this.transferForm.value;

      // Llamada al Backend Java
      this.accountService.transfer(targetCurp, amount, description || '').subscribe({
        next: (response) => {
          this.loading = false;
          
          this.messageService.add({
            severity: 'success', 
            summary: '¡Éxito!', 
            detail: `Transferencia de $${amount} enviada correctamente.`
          });

          // Limpiar formulario y redirigir tras 1.5 seg
          this.transferForm.reset();
          setTimeout(() => {
            this.router.navigate(['/client/dashboard']);
          }, 1500);
        },
        error: (err) => {
          this.loading = false;
          console.error(err);
          // Mostrar mensaje de error que viene de Java (ej. "Fondos insuficientes")
          const errorMsg = err.error?.error || 'Ocurrió un error en la transferencia';
          
          this.messageService.add({
            severity: 'error', 
            summary: 'Error', 
            detail: errorMsg
          });
        }
      });
    } else {
      this.messageService.add({severity:'warn', summary:'Atención', detail:'Verifica los datos del formulario'});
    }
  }
}