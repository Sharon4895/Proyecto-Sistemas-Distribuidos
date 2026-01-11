import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, BehaviorSubject } from 'rxjs'; // <--- IMPRESCINDIBLE
import { User } from '../models/financial.models';
import { decodeJwt } from '../utils/jwt.util';
import { environment } from '../../../environments/environment';
@Injectable({ providedIn: 'root' })
export class AuthService {
      // Obtener datos del usuario desde el JWT
      getUserFromToken(): any {
        const token = localStorage.getItem('auth_token');
        if (!token) return null;
        return decodeJwt(token);
      }
    // Registro de usuario
    register(curp: string, password: string, name: string) {
      return this.http.post<any>(`${this.apiUrl}/register`, { curp, password, name });
    }

    private apiUrl = environment.apiUrl + '/auth';

  // Fuente de verdad: solo el token
  private currentTokenSubject: BehaviorSubject<string | null>;
  public currentToken$: Observable<string | null>;

  constructor(private http: HttpClient) {
    // Al iniciar, leemos el token si existe
    const storedToken = localStorage.getItem('auth_token');
    this.currentTokenSubject = new BehaviorSubject<string | null>(storedToken);
    this.currentToken$ = this.currentTokenSubject.asObservable();
  }

  login(curp: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, { curp, password }).pipe(
      tap(response => {
        if (response.success && response.token) {
          localStorage.setItem('auth_token', response.token);
          this.currentTokenSubject.next(response.token);
        }
      })
    );
  }

  logout() {
    localStorage.removeItem('auth_token');
    this.currentTokenSubject.next(null);
  }

  // Método auxiliar simple (no reactivo)
  getCurrentUser(): User | null {
    // Eliminado: ya no se guarda el usuario completo
    return null;
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem('auth_token');
  }

  // 3. ¿Qué rol tiene? (Devuelve 'ADMIN' o 'USER')
  getUserRole(): string {
    // Eliminado: ya no se guarda el rol en frontend
    return '';
  }
}