import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { PublicGameStateModel } from '../../../../shared/models/game-state.models';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-match-header',
  standalone: true,
  imports: [LoadingSpinnerComponent],
  template: `
    @if (publicState(); as state) {
      <header class="flex gap-4 items-center p-3 px-6 text-[var(--pk-text-bright)] rounded-lg" [style.background]="'color-mix(in oklch, var(--pk-surface) 75%, transparent)'">
        <span class="font-semibold">Turno {{ state.turnNumber }}</span>
        <span class="text-[var(--pk-text-dim)]">Fase: {{ state.phase }}</span>
        @if (state.currentPlayerId === myPlayerId()) {
          <span class="ml-auto font-medium text-green-500">⭐ Es tu turno</span>
        } @else {
          <span class="ml-auto font-medium text-amber-400">⏳ Esperando oponente</span>
        }
      </header>
    } @else {
      <app-loading-spinner />
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MatchHeaderComponent {
  readonly publicState = input<PublicGameStateModel | null>(null);
  readonly myPlayerId = input<string | null>(null);
}
