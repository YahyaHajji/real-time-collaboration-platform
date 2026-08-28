import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <main class="relay-login">
      <section class="login-visual" aria-labelledby="login-promise">
        <div class="relay-brand">
          <span class="logo-tile"><img src="relay-logo.svg" alt=""></span>
          <span><strong>Relay</strong><small>Real-time collaboration</small></span>
        </div>

        <div class="login-promise">
          <p class="section-kicker">Move work forward together</p>
          <p class="hero-title" id="login-promise">Your team, in the moment.</p>
          <p>One focused place for conversations, decisions, notifications, and the people moving work forward.</p>
        </div>

        <div class="login-network" aria-hidden="true">
          <div class="network-preview">
            <span>Project room · live</span>
            <div><i class="preview-avatar">AM</i><b></b></div>
          </div>
          <div class="network-preview elevated">
            <span class="preview-live"><i></i>3 teammates online</span>
            <div><i class="preview-avatar">YU</i><b></b></div>
          </div>
        </div>
      </section>

      <section class="login-form-region" aria-labelledby="login-title">
        <form class="relay-login-card" (ngSubmit)="submit()" novalidate>
          <div class="relay-brand mobile-login-brand">
            <span class="logo-tile"><img src="relay-logo.svg" alt=""></span>
            <span><strong>Relay</strong><small>Real-time collaboration</small></span>
          </div>
          <p class="section-kicker">Secure workspace access</p>
          <h1 id="login-title">Welcome back</h1>
          <p class="login-intro">Sign in to continue to your team workspace.</p>

          <div class="form-field">
            <label for="email">Work email</label>
            <input id="email" name="email" type="email" autocomplete="username"
                   placeholder="name@company.com" [(ngModel)]="email" required>
          </div>
          <div class="form-field">
            <label for="password">Password</label>
            <input id="password" name="password" type="password" autocomplete="current-password"
                   placeholder="Enter your password" [(ngModel)]="password" required
                   [attr.aria-describedby]="error() ? 'login-error' : null">
          </div>
          @if (error()) {
            <p id="login-error" class="form-alert" role="alert">{{ error() }}</p>
          }
          <button class="primary-action" type="submit" [disabled]="loading()">
            {{ loading() ? 'Signing in…' : 'Sign in' }}
          </button>
          <p class="security-note">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 11V8a5 5 0 0 1 10 0v3"/><rect x="5" y="11" width="14" height="10" rx="2"/></svg>
            Your session is protected with secure workspace authentication.
          </p>
        </form>
      </section>
    </main>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  email = '';
  password = '';
  readonly loading = signal(false);
  readonly error = signal('');

  constructor() {
    if (this.auth.isAuthenticated()) void this.router.navigate(['/']);
  }

  submit(): void {
    if (!this.email.trim() || !this.password) {
      this.error.set('Enter your work email and password.');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.auth.login({ email: this.email, password: this.password })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/']),
        error: (failure: HttpErrorResponse) => this.error.set(
          failure.status === 401 ? 'The email or password is incorrect.' : 'Sign-in is unavailable right now.'
        )
      });
  }
}
