import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { BackButtonComponent } from '../../../../shared/components/back-button/back-button.component';
import { DeckApiService } from '../../../../core/api/deck-api.service';
import { AuthService } from '../../../../core/services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { DeckResponse } from '../../../../shared/models/deck.models';
import { DeckListComponent } from './deck-list.component';
import { ImportDeckModalComponent } from './import-deck-modal.component';
import { NewDeckModalComponent } from './new-deck-modal.component';
import { PredefinedDeckModalComponent } from './predefined-deck-modal.component';

interface DeckDataState {
  decks: DeckResponse[];
  loading: boolean;
  error: string | null;
}

@Component({
  selector: 'app-deck-list-page',
  imports: [DeckListComponent, BackButtonComponent, ImportDeckModalComponent, NewDeckModalComponent, PredefinedDeckModalComponent],
  template: `
    <div class="relative min-h-dvh">
      <app-back-button route="/home" />
      <main class="min-h-dvh bg-[var(--pk-bg)] p-8 max-w-4xl mx-auto">
        <h1 class="m-0 text-lg text-[var(--pk-text)] mb-6">MIS MAZOS</h1>

        <div class="mb-6 flex items-center gap-3">
          <input
            type="text"
            placeholder="Buscar mazos..."
            (input)="searchQuery.set($any($event.target).value)"
            class="w-full max-w-lg rounded-md border border-[var(--pk-btn-border)] bg-[var(--pk-surface)] px-3 py-2 text-[length:var(--pk-fz-base)] text-[var(--pk-text)] placeholder-[var(--pk-text-dim)] focus:border-[var(--pk-accent)] focus:outline-none focus:ring-1 focus:ring-[var(--pk-accent)]"
          />
          <button class="pk-btn" (click)="showImportModal.set(true)">+ Importar</button>
          <button class="pk-btn" (click)="showNewDeckModal.set(true)">+ Nuevo mazo</button>
        </div>

        <app-deck-list
          [decks]="filteredDecks()"
          [loading]="deckData().loading"
          [error]="deckData().error"
          [empty]="filteredDecks().length === 0 && !deckData().loading && !deckData().error"
          (delete)="onDelete($event)"
          (play)="onPlay($event)"
          (edit)="onEdit($event)"
          (retry)="onRetry()"
        />
        @if (showImportModal()) {
          <app-import-deck-modal
            (close)="showImportModal.set(false)"
            (imported)="onImportComplete($event)"
          />
        }
        @if (showNewDeckModal()) {
          <app-new-deck-modal
            (close)="showNewDeckModal.set(false)"
            (predefined)="onPredefined()"
            (fromScratch)="onFromScratch()"
          />
        }
        @if (showPredefinedModal()) {
          <app-predefined-deck-modal
            (close)="showPredefinedModal.set(false)"
            (back)="onBackToNewDeck()"
            (copied)="onPredefinedCopied()"
          />
        }
      </main>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckListPage {
  private readonly deckApi = inject(DeckApiService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly showImportModal = signal(false);
  protected readonly showNewDeckModal = signal(false);
  protected readonly showPredefinedModal = signal(false);
  protected readonly searchQuery = signal('');
  protected readonly deckData = signal<DeckDataState>({ decks: [], loading: false, error: null });

  protected readonly filteredDecks = computed(() => {
    const q = this.searchQuery().toLowerCase();
    if (!q) return this.deckData().decks;
    return this.deckData().decks.filter(d => d.name.toLowerCase().includes(q));
  });

  constructor() {
    this.loadDecks();
  }

  private loadDecks(): void {
    const playerId = this.authService.playerId();
    if (!playerId) {
      this.deckData.set({ decks: [], loading: false, error: 'No autenticado.' });
      return;
    }
    this.deckData.update(s => ({ ...s, loading: true, error: null }));
    this.deckApi.listByPlayer(playerId).subscribe({
      next: (decks) => this.deckData.set({ decks, loading: false, error: null }),
      error: (err) =>
        this.deckData.set({
          decks: [],
          loading: false,
          error: err?.error?.message || 'Error al cargar mazos.',
        }),
    });
  }

  onRetry(): void {
    this.loadDecks();
  }

  protected onPredefined(): void {
    this.showPredefinedModal.set(true);
  }

  protected onBackToNewDeck(): void {
    this.showPredefinedModal.set(false);
    this.showNewDeckModal.set(true);
  }

  protected onFromScratch(): void {
    const playerId = this.authService.playerId();
    if (playerId) {
      this.router.navigate(['/decks/new'], { queryParams: { playerId } });
    }
  }

  protected onPredefinedCopied(): void {
    this.loadDecks();
  }

  onEdit(id: string): void {
    this.router.navigate(['/decks', id, 'edit']);
  }

  onPlay(id: string): void {
    this.router.navigate(['/lobby'], { queryParams: { deckId: id } });
  }

  onImportComplete(_count: number): void {
    this.showImportModal.set(false);
    this.loadDecks();
  }

  onDelete(id: string): void {
    this.deckApi.delete(id).subscribe({
      next: () => {
        this.notificationService.show('Mazo eliminado', 'success');
        this.loadDecks();
      },
      error: () => {
        this.notificationService.show('Error al eliminar el mazo', 'error');
      },
    });
  }

}
