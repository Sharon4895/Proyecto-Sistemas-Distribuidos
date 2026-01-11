import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Transaction, User } from '../models/financial.models';

@Injectable({ providedIn: 'root' })
export class AccountService {

  private apiUrl = 'http://localhost:8080/api'; // URL Java

  constructor(private http: HttpClient) { }

  // Obtener token del usuario logueado
  private getToken(): string {
    return localStorage.getItem('auth_token') || '';
  }

  // --- CONSULTAR SALDO REAL ---
  getBalance(): Observable<any> { // Cambiamos return a 'any' porque el back devuelve {balance: N}
    const headers = new HttpHeaders().set('Authorization', `Bearer ${this.getToken()}`);
    return this.http.get<{balance: number}>(`${this.apiUrl}/account/balance`, { headers })
      .pipe(
        tap(res => console.log('Saldo real recibido:', res))
      );
  }

  // --- CONSULTAR HISTORIAL REAL ---
  getTransactions(): Observable<Transaction[]> {
    const headers = new HttpHeaders().set('Authorization', `Bearer ${this.getToken()}`);
    return this.http.get<Transaction[]>(`${this.apiUrl}/transactions`, { headers });
  }

  // --- OPERAR REALMENTE ---
  operate(type: 'DEPOSIT' | 'WITHDRAW', amount: number): Observable<any> {
    const body = {
      type: type,
      amount: amount
    };
    const headers = new HttpHeaders().set('Authorization', `Bearer ${this.getToken()}`);
    return this.http.post(`${this.apiUrl}/account/operate`, body, { headers });
  }

  // En AccountService
  transfer(targetCurp: string, amount: number, description: string): Observable<any> {
    const body = {
      targetCurp: targetCurp,
      amount: amount,
      description: description
    };
    const headers = new HttpHeaders().set('Authorization', `Bearer ${this.getToken()}`);
    return this.http.post(`${this.apiUrl}/account/transfer`, body, { headers });
  }
}