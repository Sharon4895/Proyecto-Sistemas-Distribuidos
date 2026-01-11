import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, BehaviorSubject } from 'rxjs'; // <--- IMPRESCINDIBLE
import { User } from '../models/financial.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
    // Registro de usuario
    register(curp: string, password: string, name: string) {
      return this.http.post<any>(`${this.apiUrl}/register`, { curp, password, name });
    }
  
  private apiUrl = 'http://localhost:8080/api/auth'; 

  // 1. FUENTE DE VERDAD (Aquí se guarda el estado actual)
  private currentUserSubject: BehaviorSubject<User | null>;
  
  // 2. OBSERVABLE PÚBLICO (A esto se suscribe la Navbar)
  // Nota: Se llama 'currentUser$' (sin el 'get')
  public currentUser$: Observable<User | null>;

  constructor(private http: HttpClient) {
    // Al iniciar, leemos si ya había alguien en localStorage
    const storedUser = localStorage.getItem('current_user');
    const user = storedUser ? JSON.parse(storedUser) : null;

    // Inicializamos el Subject
    this.currentUserSubject = new BehaviorSubject<User | null>(user);
    this.currentUser$ = this.currentUserSubject.asObservable();
  }

  login(curp: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, { curp, password }).pipe(
      tap(response => {
        if (response.success) {
          const userToSave: User = { 
            curp: curp, 
            name: response.name, 
            role: response.role, 
            token: response.token 
          };

          localStorage.setItem('current_user', JSON.stringify(userToSave));
          
          // 3. ¡AVISAMOS A LA APP! (Esto actualiza la Navbar)
          this.currentUserSubject.next(userToSave);
        }
      })
    );
  }

  logout() {
    localStorage.removeItem('current_user');
    // 4. AVISAMOS QUE SALIÓ (Esto borra el nombre en la Navbar)
    this.currentUserSubject.next(null);
  }

  // Método auxiliar simple (no reactivo)
  getCurrentUser(): User | null {
    const userStr = localStorage.getItem('current_user');
    return userStr ? JSON.parse(userStr) : null;
  }

  isAuthenticated(): boolean {
    return !!this.getCurrentUser();
  }

  // 3. ¿Qué rol tiene? (Devuelve 'ADMIN' o 'USER')
  getUserRole(): string {
    const user = this.getCurrentUser();
    return user ? user.role : '';
  }
}