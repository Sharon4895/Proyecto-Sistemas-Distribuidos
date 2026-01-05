import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private apiUrl = 'http://localhost:8080/api/admin';

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
  getUserLogs(userId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/user-logs?userId=${userId}`);
  }
}