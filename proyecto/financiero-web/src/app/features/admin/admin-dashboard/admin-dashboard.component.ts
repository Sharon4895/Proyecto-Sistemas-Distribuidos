import { Component, OnInit, OnDestroy, QueryList, ViewChildren } from '@angular/core'; // <--- 1. Importar QueryList y ViewChildren
import { CommonModule } from '@angular/common';
import { NgChartsModule, BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions, Chart, registerables } from 'chart.js';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { RouterModule } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { interval, Subscription } from 'rxjs';

Chart.register(...registerables);

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, NgChartsModule, CardModule, TableModule, ButtonModule, RouterModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss'
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  
  // 2. CAMBIO: Usamos ViewChildren (PLURAL) para controlar TODAS las gráficas
  @ViewChildren(BaseChartDirective) charts: QueryList<BaseChartDirective> | undefined;

  stats = { totalUsers: 0, totalMoney: 0 ,todayTx:0};
  users: any[] = [];
  loading = true;
  private updateSubscription: Subscription | null = null;

  // --- GRÁFICA LINEAL (Transacciones por Hora) ---
  public lineChartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Transacciones (Hoy)',
        fill: true,
        tension: 0.4,
        borderColor: '#2563eb',
        backgroundColor: 'rgba(37, 99, 235, 0.2)'
      }
    ]
  };
  public lineChartOptions: ChartOptions<'line'> = { responsive: true, maintainAspectRatio: false };

  // --- GRÁFICA BARRAS (Volumen 7 días) ---
  public barChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [], 
    datasets: [
      { 
        data: [], 
        label: 'Volumen ($ MXN)',
        backgroundColor: '#10b981',
        borderRadius: 5
      }
    ]
  };
  public barChartOptions: ChartOptions<'bar'> = { responsive: true, maintainAspectRatio: false };

  constructor(private adminService: AdminService) {}

  ngOnInit() {
    this.loadAllData();
    this.updateSubscription = interval(5000).subscribe(() => {
      this.loadAllData();
    });
  }

  ngOnDestroy() {
    if (this.updateSubscription) {
      this.updateSubscription.unsubscribe();
    }
  }

  loadAllData() {
    // Carga de Stats y Usuarios (Igual que antes)
    this.adminService.getStats().subscribe(data => this.stats = data);
    this.adminService.getUsers().subscribe(data => { this.users = data; this.loading = false; });

    // Carga de Gráficas
    this.adminService.getDashboardCharts().subscribe(chartData => {
      
      // A. Actualizar Datos de LÍNEA
      this.lineChartData.labels = chartData.line.labels.map((h: number) => `${h}:00`);
      this.lineChartData.datasets[0].data = chartData.line.data;

      // B. Actualizar Datos de BARRAS (Volumen)
      // Aseguramos que labels y data sean arrays, aunque vengan vacíos
      this.barChartData.labels = chartData.bar.labels || [];
      this.barChartData.datasets[0].data = chartData.bar.data || [];

      // 3. CAMBIO: Forzar actualización de AMBAS gráficas
      if (this.charts) {
        this.charts.forEach((child) => {
          child.update();
        });
      }
    });
  }
}