import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DeckValidationModel } from '../../../../shared/models/deck.models';
import { DeckRule } from '../../services/deck-builder-facade.service';

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

      <div class="mb-3 border-t border-[var(--pk-btn-border)] pt-2">
        <h4 class="mb-1 text-xs font-semibold text-[var(--pk-text)]">Reglas</h4>
        <div class="space-y-0.5 text-xs">
          @for (rule of rules(); track rule.code) {
            <div class="flex items-center gap-1.5">
              @if (rule.valid) {
                <span class="text-green-600">&#10003;</span>
              } @else {
                <span class="text-red-600">&#10007;</span>
              }
              <span [class.text-green-700]="rule.valid" [class.text-red-700]="!rule.valid">
                {{ rule.label }}
              </span>
            </div>
          }
        </div>
      </div>

      @if (validation(); as v) {
        @if (v.valid) {
          <p class="text-sm font-medium text-green-700">&#10003; Listo para jugar.</p>
        } @else {
          <div class="space-y-1">
            <p class="text-sm font-medium text-red-700">&#10007; Inválido</p>
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
  rules = input<DeckRule[]>([]);
}
