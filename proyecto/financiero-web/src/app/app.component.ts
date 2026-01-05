import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { FooterComponent } from './shared/components/footer/footer.component';
// 1. Importar ToastModule
import { ToastModule } from 'primeng/toast'; 

@Component({
  selector: 'app-root',
  standalone: true,
  // 2. Agregar ToastModule a los imports
  imports: [CommonModule, RouterOutlet, NavbarComponent, FooterComponent, ToastModule], 
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'financiero-web';
}