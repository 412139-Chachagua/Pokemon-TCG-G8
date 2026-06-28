import { ChangeDetectionStrategy, Component, inject, input, output, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { DeckApiService } from '../../../../core/api/deck-api.service';
import { MatchFacadeService } from '../../../match/services/match-facade.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';
import { DeckResponse } from '../../../../shared/models/deck.models';
import { MatchResponse } from '../../../../core/api/match-api.service';

@Component({
  selector: 'app-match-join',
  standalone: true,
  imports: [FormsModule, LoadingSpinnerComponent],
  template: `
    <div class="p-4 border border-[var(--pk-dark)] rounded-lg">
      <h3 class="m-0 mb-4 text-lg text-[var(--pk-text)]">UNIRSE A PARTIDA</h3>

      @if (loading() && decks().length === 0) {
        <app-loading-spinner />
      } @else {
        <div class="mb-6">
          @if (selectedOpponent(); as opponent) {
            <p class="text-[var(--pk-text)] text-sm">
              Unirse a partida de <strong>{{ opponent }}</strong> (Nivel: 1)
            </p>
          } @else {
            <p class="text-[var(--pk-text-dim)] text-sm">Seleccioná una partida de la lista</p>
          }
        </div>

        <div class="mb-3">
          <label for="join-name" class="block mb-1 text-sm text-[var(--pk-text-dim)]">Tu nombre:</label>
          <input
            id="join-name"
            type="text"
            [(ngModel)]="playerName"
            class="w-full p-2 border border-[var(--pk-btn-border)] rounded-md bg-[var(--pk-surface)] text-[var(--pk-text)] placeholder-[var(--pk-text-dim)] box-border"
            placeholder="Ingresá tu nombre"
          />
        </div>

        <div class="mb-3">
          <label for="join-deck" class="block mb-1 text-sm text-[var(--pk-text-dim)]">Mazo:</label>
          <select
            id="join-deck"
            [(ngModel)]="selectedDeckId"
            class="w-full p-2 border border-[var(--pk-btn-border)] rounded-md bg-[var(--pk-surface)] text-[var(--pk-text)] box-border"
          >
            <option [ngValue]="null" disabled>Seleccioná un mazo</option>
            @for (deck of decks(); track deck.id) {
              <option [ngValue]="deck.id">{{ deck.name }}</option>
            }
          </select>
        </div>

        @if (error()) {
          <p class="text-[var(--pk-error)] text-[0.6rem] my-2">{{ error() }}</p>
        }

        <button
          class="pk-btn"
          [disabled]="loading() || !matchIdField() || !playerName() || !selectedDeckId()"
          (click)="onSubmit()"
        >
          {{ loading() ? 'Uniéndose...' : 'Unirse' }}
        </button>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MatchJoinComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly deckApi = inject(DeckApiService);
  private readonly matchFacade = inject(MatchFacadeService);
  private readonly notification = inject(NotificationService);

  constructor() {
    const name = this.authService.player()?.displayName;
    if (name) this.playerName.set(name);
  }

  playerId = input.required<string>();
  joined = output<MatchResponse>();

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly decks = signal<DeckResponse[]>([]);
  readonly matchIdField = signal('');
  readonly selectedOpponent = signal<string | null>(null);
  readonly playerName = signal('');
  readonly selectedDeckId = signal<string | null>(null);

  ngOnInit(): void {
    this.deckApi.listByPlayer(this.playerId()).subscribe({
      next: (decks) => {
        const validDecks = decks.filter(d => d.valid);
        this.decks.set(validDecks);
        if (validDecks.length === 1) {
          this.selectedDeckId.set(validDecks[0].id);
        }
      },
      error: (err) => {
        this.error.set('Error al cargar los mazos');
        this.notification.show('No se pudieron cargar los mazos', 'error');
      },
    });
  }

  selectMatch(id: string, hostName: string): void {
    this.matchIdField.set(id);
    this.selectedOpponent.set(hostName);
  }

  onSubmit(): void {
    const matchId = this.matchIdField();
    const name = this.playerName();
    const deckId = this.selectedDeckId();
    if (!matchId || !name || !deckId) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.matchFacade.joinMatch(matchId, name, deckId).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.joined.emit(response);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.message ?? 'Error al unirse a la partida');
        this.notification.show(this.error()!, 'error');
      },
    });
  }
}
