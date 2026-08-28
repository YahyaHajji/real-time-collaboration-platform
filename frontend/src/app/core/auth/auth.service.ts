import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, CurrentUser, LoginRequest, Role } from './auth.models';

interface StoredSession {
  accessToken: string;
  expiresAt: string;
  user: CurrentUser;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly storageKey = 'collaboration.session';
  private readonly session = signal<StoredSession | null>(this.restore());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly token = computed(() => this.session()?.accessToken ?? null);
  readonly expiresAt = computed(() => this.session()?.expiresAt ?? null);
  readonly isAuthenticated = computed(() => {
    const value = this.session();
    return !!value && Date.parse(value.expiresAt) > Date.now();
  });

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, request).pipe(
      tap((response) => {
        const stored: StoredSession = {
          accessToken: response.accessToken,
          expiresAt: response.expiresAt,
          user: {
            id: response.id,
            email: response.email,
            displayName: response.displayName,
            role: response.role
          }
        };
        sessionStorage.setItem(this.storageKey, JSON.stringify(stored));
        this.session.set(stored);
      })
    );
  }

  logout(redirect = true): void {
    sessionStorage.removeItem(this.storageKey);
    this.session.set(null);
    if (redirect) {
      void this.router.navigate(['/login']);
    }
  }

  hasRole(role: Role): boolean {
    return this.user()?.role === role;
  }

  private restore(): StoredSession | null {
    try {
      const raw = sessionStorage.getItem(this.storageKey);
      if (!raw) return null;
      const stored = JSON.parse(raw) as StoredSession;
      if (!stored.accessToken || Date.parse(stored.expiresAt) <= Date.now()) {
        sessionStorage.removeItem(this.storageKey);
        return null;
      }
      return stored;
    } catch {
      sessionStorage.removeItem(this.storageKey);
      return null;
    }
  }
}

