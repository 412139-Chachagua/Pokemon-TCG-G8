import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-deck-validation',
  template: `
    @if (valid() === true) {
      <span class="inline-flex items-center gap-1 rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800">
        <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
        Válido
      </span>
    } @else if (valid() === false) {
      <span class="inline-flex items-center gap-1 rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-medium text-red-800">
        <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
        Inválido
      </span>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckValidationComponent {
  valid = input<boolean | null>(null);
}
