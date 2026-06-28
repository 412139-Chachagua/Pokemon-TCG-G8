import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { Location } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-back-button',
  standalone: true,
  template: `
    <button
      class="pk-btn pk-btn--back absolute left-4 top-6"
      (click)="onClick()"
    >
      ← VOLVER
    </button>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BackButtonComponent {
  private readonly location = inject(Location);
  private readonly router = inject(Router);

  /**
   * Si se especifica, navega a esta ruta en vez de usar Location.back().
   */
  readonly route = input<string | null>(null);

  /**
   * Se emite ANTES de la navegación.
   * Útil para hacer cleanup (reset de estado, flags, etc.)
   */
  readonly beforeBack = output<void>();

  protected onClick(): void {
    this.beforeBack.emit();
    const target = this.route();
    if (target) {
      this.router.navigate([target], { replaceUrl: true });
    } else {
      this.location.back();
    }
  }
}
