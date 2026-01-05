import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Transaction, User } from '../models/financial.models';

@Injectable({ providedIn: 'root' })
export class AccountService {

  private apiUrl = 'http://localhost:8080/api'; // URL Java

  constructor(private http: HttpClient) { }

  // Obtener CURP del usuario logueado (guardado en localStorage por AuthService)
  private getCurp(): string {
    const userStr = localStorage.getItem('current_user');
    if (userStr) {
      const user = JSON.parse(userStr);
      return user.curp || '';
    }
    return '';
  }

  // --- CONSULTAR SALDO REAL ---
  getBalance(): Observable<any> { // Cambiamos return a 'any' porque el back devuelve {balance: N}
    const headers = new HttpHeaders().set('X-User-Curp', this.getCurp());
    return this.http.get<{balance: number}>(`${this.apiUrl}/account/balance`, { headers })
      .pipe(
        // Extraemos solo el número de la respuesta JSON
        tap(res => console.log('Saldo real recibido:', res))
      );
  }

  // --- CONSULTAR HISTORIAL REAL ---
  getTransactions(): Observable<Transaction[]> {
    const headers = new HttpHeaders().set('X-User-Curp', this.getCurp());
    return this.http.get<Transaction[]>(`${this.apiUrl}/transactions`, { headers });
  }

  // --- OPERAR REALMENTE ---
  operate(type: 'DEPOSIT' | 'WITHDRAW', amount: number): Observable<any> {
    const body = {
      curp: this.getCurp(),
      type: type,
      amount: amount
    };
    return this.http.post(`${this.apiUrl}/account/operate`, body);
  }

  // En AccountService
  transfer(targetCurp: string, amount: number, description: string): Observable<any> {
    const body = {
      sourceCurp: this.getCurp(),
      targetCurp: targetCurp,
      amount: amount,
      description: description // <--- ¡AQUÍ SE ENVÍA AL BACKEND!
    };
    return this.http.post(`${this.apiUrl}/account/transfer`, body);
  }
}