import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BackButtonComponent } from '../../../../shared/components/back-button/back-button.component';
import { AuthService } from '../../../../core/services/auth.service';
import { DeckApiService } from '../../../../core/api/deck-api.service';
import { MatchStateService } from '../../../match/services/match-state.service';
import { MatchFacadeService } from '../../../match/services/match-facade.service';

@Component({
  selector: 'app-quick-match-page',
  standalone: true,
  imports: [FormsModule, BackButtonComponent],
  template: `
    <div class="relative min-h-dvh">
      <app-back-button />
      <main class="min-h-dvh bg-[var(--pk-bg)] p-6 max-w-4xl mx-auto">
        @if (!authService.isAuthenticated()) {
          <div class="flex flex-col items-center gap-4 text-center px-4 py-12">
            <p class="pk-text-dim">Iniciá sesión para jugar</p>
            <a routerLink="/auth/register" class="pk-btn">Ir a registro</a>
          </div>
        } @else {
          <div class="pk-panel max-w-md mx-auto mt-12">
            <div class="pk-panel__header">PARTIDA RELAMPAGO (DEBUG)</div>
            <div class="pk-panel__body">
              <p class="text-[var(--pk-text-dim)] text-sm mb-3">
                30 cartas iniciales en mano. Crea una partida y otro jugador se une desde la lista de partidas.
              </p>
              @if (quickMatchDecks().length > 0) {
                <div class="mb-3">
                  <label class="block mb-1 text-sm text-[var(--pk-text-dim)]">Mazo:</label>
                  <select [(ngModel)]="quickMatchDeckId"
                    class="w-full p-2 border border-[var(--pk-btn-border)] rounded-md bg-[var(--pk-surface)] text-[var(--pk-text)] box-border">
                    @for (deck of quickMatchDecks(); track deck.id) {
                      <option [ngValue]="deck.id">{{ deck.name }}</option>
                    }
                  </select>
                </div>
                <button class="pk-btn" (click)="onQuickMatch()" [disabled]="quickMatchLoading()">
                  {{ quickMatchLoading() ? 'Creando...' : 'Buscar partida relámpago' }}
                </button>
              } @else {
                <p class="text-[var(--pk-text-dim)] text-xs">Necesitás al menos un mazo válido.</p>
              }
            </div>
          </div>
        }
      </main>
    </div>
  `,
  styles: [`
    :host { animation: pk-fade-in 0.35s ease-out both; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuickMatchPage implements OnInit {
  private readonly matchState = inject(MatchStateService);
  private readonly matchFacade = inject(MatchFacadeService);
  private readonly router = inject(Router);
  private readonly deckApi = inject(DeckApiService);

  protected readonly authService = inject(AuthService);
  protected readonly quickMatchDecks = signal<{ id: string; name: string }[]>([]);
  protected readonly quickMatchDeckId = signal<string | null>(null);
  protected readonly quickMatchLoading = signal(false);

  ngOnInit(): void {
    this.deckApi.listByPlayer(this.authService.playerId() ?? '').subscribe({
      next: (decks) => {
        const valid = decks.filter(d => d.valid).map(d => ({ id: d.id, name: d.name }));
        this.quickMatchDecks.set(valid);
        if (valid.length > 0) {
          this.quickMatchDeckId.set(valid[0].id);
        }
      },
    });
  }

  protected onQuickMatch(): void {
    const deckId = this.quickMatchDeckId();
    if (!deckId) return;
    this.quickMatchLoading.set(true);
    this.matchFacade.createQuickMatch(deckId).subscribe({
      next: (res) => {
        this.quickMatchLoading.set(false);
        this.onMatchCreated(res);
      },
      error: () => {
        this.quickMatchLoading.set(false);
      },
    });
  }

  private onMatchCreated(response: { id: string }): void {
    this.matchState.reset();
    this.matchFacade.reset();
    this.router.navigate(['/match', response.id], { replaceUrl: true });
  }
}
