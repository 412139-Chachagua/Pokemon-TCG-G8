import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly form: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly deactivatedUserId = signal<string | null>(null);
  readonly reactivateLoading = signal(false);

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.deactivatedUserId.set(null);

    const { email, password } = this.form.value;
    this.authService.login(email, password).subscribe({
      next: () => {
        this.notificationService.show('Inicio de sesión exitoso', 'success');
        setTimeout(() => this.router.navigate(['/welcome']), 1500);
      },
      error: (err) => {
        this.loading.set(false);
        const statusCode = err.status;
        const message = err.error?.message || '';

        if (statusCode === 403 && message.startsWith('Account deactivated|')) {
          const userId = message.split('|')[1];
          this.deactivatedUserId.set(userId);
          this.errorMessage.set('Tu cuenta está desactivada');
        } else {
          this.errorMessage.set(message || 'Error al iniciar sesión. Intentá de nuevo.');
        }
      },
    });
  }

  onReactivate(): void {
    const userId = this.deactivatedUserId();
    const password = this.form.value.password;
    if (!userId || !password) return;

    this.reactivateLoading.set(true);
    this.errorMessage.set(null);

    this.authService.activateUser(userId, password).subscribe({
      next: () => {
        this.notificationService.show('Cuenta reactivada. Iniciando sesión...', 'success');
        this.deactivatedUserId.set(null);
        this.onSubmit();
      },
      error: (err) => {
        this.reactivateLoading.set(false);
        const message = err.error?.message || 'Error al reactivar la cuenta';
        this.errorMessage.set(message);
      },
    });
  }
}
