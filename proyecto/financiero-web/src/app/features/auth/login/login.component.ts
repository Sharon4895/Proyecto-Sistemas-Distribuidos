import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

// PrimeNG Imports
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { PasswordModule } from 'primeng/password';
import { MessageService } from 'primeng/api'; // <--- Importante
import { ToastModule } from 'primeng/toast';

// Servicios
import { AuthService } from '../../../core/services/auth.service'; // <--- Importante

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    RouterModule,
    CardModule, 
    InputTextModule, 
    ButtonModule, 
    PasswordModule,
    ToastModule
  ],
  providers: [MessageService], // Proveedor para las alertas
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  
  loginForm: FormGroup;
  loading = false; // <--- Se llama 'loading', así que usaremos this.loading

  constructor(
    private fb: FormBuilder,
    private router: Router,
    // AGREGAMOS 'private' PARA QUE ESTÉN DISPONIBLES EN TODA LA CLASE
    private authService: AuthService, 
    private messageService: MessageService
  ) {
    this.loginForm = this.fb.group({
      curp: ['', [Validators.required, Validators.minLength(1)]], // Validación simple
      password: ['', [Validators.required, Validators.minLength(3)]]
    });
  }

  onSubmit() {
    if (this.loginForm.valid) {
      this.loading = true;
      const { curp, password } = this.loginForm.value;
      this.authService.login(curp, password).subscribe({
        next: () => {
          this.loading = false;
          this.messageService.add({
            severity: 'success',
            summary: 'Bienvenido',
            detail: 'Iniciando sesión...'
          });
          setTimeout(() => {
            const user = this.authService.getUserFromToken();
            if (user?.role === 'ADMIN') {
              this.router.navigate(['/admin/dashboard']);
            } else {
              this.router.navigate(['/client/dashboard']);
            }
          }, 1000);
        },
        error: (err: any) => {
          this.loading = false;
          let msg = 'Ocurrió un error';
          if (err.status === 401) {
            msg = 'Datos incorrectos. Verifica tu CURP y contraseña.';
          } else if (err.error?.message) {
            msg = err.error.message;
          }
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: msg
          });
        }
      });
    } else {
      this.messageService.add({
        severity: 'warn',
        summary: 'Atención',
        detail: 'Por favor completa todos los campos correctamente'
      });
    }
  }
}