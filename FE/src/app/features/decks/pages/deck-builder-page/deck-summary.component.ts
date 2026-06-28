import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DeckValidationModel } from '../../../../shared/models/deck.models';

@Component({
  selector: 'app-deck-summary',
  template: `
    <div class="rounded-lg border border-[var(--pk-btn-border)] bg-[var(--pk-surface)] p-4">
      <div class="mb-2 flex items-center justify-between">
        <h3 class="text-sm font-semibold text-[var(--pk-text)]">Resumen</h3>
        <span class="text-lg font-bold text-[var(--pk-text)]" [class.text-red-600]="totalCards() !== 60">
          {{ totalCards() }} / 60
        </span>
      </div>

      <div class="mb-2 space-y-1 text-xs">
        <div class="flex justify-between" [class.text-red-600]="basicPokemonCount() < 4" [class.text-green-600]="basicPokemonCount() >= 4">
          <span>Pokémon Básico</span>
          <span>{{ basicPokemonCount() }} / 4</span>
        </div>
        <div class="flex justify-between text-[var(--pk-text)]">
          <span>Pokémon</span>
          <span>{{ pokemonCount() }}</span>
        </div>
        <div class="flex justify-between text-[var(--pk-text)]">
          <span>Entrenadores</span>
          <span>{{ trainerCount() }}</span>
        </div>
        <div class="flex justify-between text-[var(--pk-text)]">
          <span>Energías</span>
          <span>{{ energyCount() }}</span>
        </div>
      </div>

      @if (validation(); as v) {
        @if (v.valid) {
          <p class="text-sm font-medium text-green-700">✅ Listo para jugar.</p>
        } @else {
          <div class="space-y-1">
            <p class="text-sm font-medium text-red-700">❌ Inválido</p>
            <ul class="list-inside list-disc text-xs text-red-600">
              @for (err of v.errors; track err.code) {
                <li>{{ err.message }}</li>
              }
            </ul>
          </div>
        }
      } @else {
        <p class="text-sm text-[var(--pk-text-dim)]">Aún no validado.</p>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckSummaryComponent {
  totalCards = input(0);
  pokemonCount = input(0);
  trainerCount = input(0);
  energyCount = input(0);
  basicPokemonCount = input(0);
  validation = input<DeckValidationModel | null>(null);
}
