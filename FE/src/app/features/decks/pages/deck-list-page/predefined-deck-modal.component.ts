import { ChangeDetectionStrategy, Component, inject, output, signal } from '@angular/core';
import { DeckApiService } from '../../../../core/api/deck-api.service';
import { AuthService } from '../../../../core/services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { DeckResponse } from '../../../../shared/models/deck.models';

@Component({
  selector: 'app-predefined-deck-modal',
  template: `
    <div
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
      (click)="onBackdropClick($event)"
    >
      <div
        class="pk-panel w-full max-w-3xl mx-4 animate-[pk-fade-in_0.2s_ease-out]"
        style="max-height: 85vh;"
      >
        <div class="flex flex-col gap-4">
          <div class="flex flex-col" style="gap: 0.15rem;">
            <div class="flex items-center gap-2">
              <button class="pk-btn pk-btn--ghost pk-btn--sm" (click)="back.emit()">
                ← Volver
              </button>
            </div>
            <h2 class="text-[var(--pk-accent)] text-[var(--pk-fz-lg)] text-center" style="margin: 0;">
              Mazos Oficiales Predefinidos
            </h2>
          </div>

          @if (loading()) {
            <div class="flex justify-center py-12">
              <div class="h-8 w-8 animate-spin rounded-full border-4 border-[var(--pk-btn-border)] border-t-[var(--pk-accent)]"></div>
            </div>
          } @else if (error()) {
            <div class="pk-panel text-center">
              <p class="text-[var(--pk-error)]">{{ error() }}</p>
              <button class="pk-btn pk-btn--danger mt-3" (click)="loadDecks()">
                Reintentar
              </button>
            </div>
          } @else {
            <div
              class="flex flex-wrap gap-4 justify-center"
              style="max-height: 60vh; overflow-y: auto; padding: 0.25rem;"
            >
              @for (deck of decks(); track deck.id) {
                <button
                  class="pk-panel flex flex-col items-center cursor-pointer hover:!border-[var(--pk-accent)] transition-colors"
                  style="width: calc(25% - 0.75rem); min-width: 180px; flex-shrink: 0; border-style: solid; background: var(--pk-surface); border-color: var(--pk-text-dim); padding: 1rem;"
                  [disabled]="copyingId() === deck.id"
                  (click)="onSelectDeck(deck)"
                >
                  @if (deck.mainCardImageUrl) {
                    <img
                      [src]="deck.mainCardImageUrl"
                      [alt]="deck.name"
                      class="w-full aspect-[3/4] object-contain rounded"
                      loading="lazy"
                    />
                  } @else {
                    <div
                      class="w-full aspect-[3/4] flex items-center justify-center rounded"
                      style="background: var(--pk-bg); color: var(--pk-text-dim); font-size: var(--pk-fz-sm);"
                    >
                      Sin imagen
                    </div>
                  }
                  <span class="mt-2 text-[var(--pk-text-bright)] font-semibold text-center" style="font-size: var(--pk-fz-base);">
                    {{ deck.name }}
                  </span>
                  <div class="mt-1 flex flex-col items-center text-[var(--pk-text-dim)]" style="font-size: var(--pk-fz-sm);">
                    <span>{{ calcPokemon(deck) }} PC</span>
                    <span>{{ calcTrainer(deck) }} ENTRENADOR</span>
                    <span>{{ calcEnergy(deck) }} ENERGÍA</span>
                  </div>
                  @if (copyingId() === deck.id) {
                    <span class="mt-2 inline-block h-4 w-4 animate-spin rounded-full border-2 border-[var(--pk-btn-border)] border-t-[var(--pk-accent)]"></span>
                  }
                </button>
              }
            </div>
          }
          <button
            class="pk-btn pk-btn--ghost justify-center"
            (click)="close.emit()"
          >
            Cancelar
          </button>
        </div>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PredefinedDeckModalComponent {
  private readonly deckApi = inject(DeckApiService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  readonly close = output<void>();
  readonly back = output<void>();
  readonly copied = output<void>();

  protected readonly decks = signal<DeckResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly copyingId = signal<string | null>(null);

  constructor() {
    this.loadDecks();
  }

  protected loadDecks(): void {
    this.loading.set(true);
    this.error.set(null);
    this.deckApi.getPredefinedDecks().subscribe({
      next: (decks) => {
        console.log('[PredefinedDecks] Response:', decks);
        this.decks.set(decks);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('[PredefinedDecks] Error:', err);
        this.loading.set(false);
        this.error.set(err?.error?.message || 'Error al cargar mazos predefinidos.');
      },
    });
  }

  protected onSelectDeck(deck: DeckResponse): void {
    const playerId = this.authService.playerId();
    if (!playerId) return;

    this.copyingId.set(deck.id);
    this.deckApi.copyDeck(deck.id, playerId).subscribe({
      next: () => {
        this.copyingId.set(null);
        this.notificationService.show(`${deck.name} añadido a tu colección`, 'success');
        this.copied.emit();
        this.close.emit();
      },
      error: () => {
        this.copyingId.set(null);
        this.notificationService.show('Error al copiar el mazo', 'error');
      },
    });
  }

  protected calcPokemon(deck: DeckResponse): number {
    return deck.cards.filter(c => c.supertype === 'POKEMON').reduce((s, c) => s + c.quantity, 0);
  }

  protected calcTrainer(deck: DeckResponse): number {
    return deck.cards.filter(c => c.supertype === 'TRAINER').reduce((s, c) => s + c.quantity, 0);
  }

  protected calcEnergy(deck: DeckResponse): number {
    return deck.cards.filter(c => c.supertype === 'ENERGY').reduce((s, c) => s + c.quantity, 0);
  }

  protected onBackdropClick(e: MouseEvent): void {
    if ((e.target as HTMLElement).classList.contains('fixed')) {
      this.close.emit();
    }
  }
}
