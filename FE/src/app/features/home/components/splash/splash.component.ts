import {
  Component,
  HostListener,
  inject,
  input,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-splash',
  templateUrl: './splash.component.html',
  styleUrls: ['./splash.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SplashComponent {
  bgImage = input<string>('assets/images/bg-lavender-town.jpeg');
  gameVersion = input<string>('VERSION GRUPO-08');
  footerText = input<string>(
    "© 2026 Nintendo. Todos los derechos reservados.\n" +
    "Pokémon® es una marca registrada de Nintendo.",
  );

  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  transitioning = signal<boolean>(false);

  @HostListener('window:keydown')
  onKeyDown(): void {
    this.handleInteraction();
  }

  @HostListener('click')
  onClick(): void {
    this.handleInteraction();
  }

  private handleInteraction(): void {
    if (this.transitioning()) return;
    this.transitioning.set(true);
    setTimeout(() => {
      const target = this.auth.isAuthenticated() ? '/home' : '/auth/login';
      this.router.navigate([target]);
    }, 450);
  }
}
