import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

// Imports UI
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, InputTextModule, PasswordModule, ButtonModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  registerForm: FormGroup;

  constructor(private fb: FormBuilder, private router: Router, private messageService: MessageService, private authService: AuthService) {
    this.registerForm = this.fb.group({
      nombre: ['', Validators.required],
      curp: ['', [Validators.required, Validators.minLength(18), Validators.maxLength(18)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    });
  }

  onSubmit() {
    if (this.registerForm.valid) {
      const { nombre, curp, password } = this.registerForm.value;
      this.authService.register(curp, password, nombre).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: '¡Bienvenido!',
            detail: 'Cuenta creada correctamente. Por favor inicia sesión.',
            life: 3000
          });
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 1000);
        },
        error: (err) => {
          const errorMsg = err.error?.error || 'Ocurrió un error al registrar';
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: errorMsg,
            life: 3000
          });
        }
      });
    } else {
      this.messageService.add({
        severity: 'warn',
        summary: 'Atención',
        detail: 'Por favor completa todos los campos correctamente.',
        life: 3000
      });
    }
  }
}