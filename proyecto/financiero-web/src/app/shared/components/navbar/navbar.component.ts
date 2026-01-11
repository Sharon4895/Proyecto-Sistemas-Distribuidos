import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router'; // Importar RouterModule para routerLink
import { ButtonModule } from 'primeng/button';

import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../../core/models/financial.models';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, ButtonModule, RouterModule], // Agregar RouterModule
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent implements OnInit {
  isAuthenticated = false;
  userName: string | null = null;
  private tokenSubscription: any;

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit() {
    this.tokenSubscription = this.authService.currentToken$.subscribe(token => {
      this.isAuthenticated = !!token;
      const user = this.authService.getUserFromToken();
      this.userName = user?.name || null;
    });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  ngOnDestroy() {
    if (this.tokenSubscription) {
      this.tokenSubscription.unsubscribe();
    }
  }
}