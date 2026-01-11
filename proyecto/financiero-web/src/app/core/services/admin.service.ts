import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
@Injectable({ providedIn: 'root' })
export class AdminService {
  private apiUrl = environment.apiUrl + '/admin';

  constructor(private http: HttpClient) {}

  // Obtener estadísticas (Dinero total, cant. usuarios)
  getStats(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats`);
  }

  // Obtener lista completa de usuarios
  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users`);
  }
  getDashboardCharts(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/charts`);
  }
  getUserLogs(userId: string | number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/user-logs?userId=${userId}`);
  }
}