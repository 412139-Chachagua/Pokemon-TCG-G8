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
  selector: 'app-match-create',
  standalone: true,
  imports: [FormsModule, LoadingSpinnerComponent],
  template: `
    <div class="p-4 border border-[var(--pk-dark)] rounded-lg">
      <h3 class="m-0 mb-4 text-lg text-[var(--pk-text)]">CREAR PARTIDA</h3>

      @if (loading() && decks().length === 0) {
        <app-loading-spinner />
      } @else {
        <div class="mb-3">
          <label for="create-name" class="block mb-1 text-sm text-[var(--pk-text-dim)]">Tu nombre:</label>
          <input
            id="create-name"
            type="text"
            [(ngModel)]="playerName"
            class="w-full p-2 border border-[var(--pk-btn-border)] rounded-md bg-[var(--pk-surface)] text-[var(--pk-text)] placeholder-[var(--pk-text-dim)] box-border"
            placeholder="Ingresá tu nombre"
          />
        </div>

        <div class="mb-3">
          <label for="create-deck" class="block mb-1 text-sm text-[var(--pk-text-dim)]">Mazo:</label>
          <select
            id="create-deck"
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
          <p class="text-red-600 text-[0.6rem] my-2">{{ error() }}</p>
        }

        <button
          class="pk-btn"
          [disabled]="loading() || !playerName() || !selectedDeckId()"
          (click)="onSubmit()"
        >
          {{ loading() ? 'Creando...' : 'Crear partida' }}
        </button>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MatchCreateComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly deckApi = inject(DeckApiService);
  private readonly matchFacade = inject(MatchFacadeService);
  private readonly notification = inject(NotificationService);

  constructor() {
    const name = this.authService.player()?.displayName;
    if (name) this.playerName.set(name);
  }

  playerId = input.required<string>();
  preSelectedDeckId = input<string | null>(null);
  created = output<MatchResponse>();

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly decks = signal<DeckResponse[]>([]);
  readonly playerName = signal('');
  readonly selectedDeckId = signal<string | null>(null);

  ngOnInit(): void {
    this.deckApi.listByPlayer(this.playerId()).subscribe({
      next: (decks) => {
        const validDecks = decks.filter(d => d.valid);
        this.decks.set(validDecks);
        const preSelected = this.preSelectedDeckId();
        if (preSelected && validDecks.some(d => d.id === preSelected)) {
          this.selectedDeckId.set(preSelected);
        } else if (validDecks.length === 1) {
          this.selectedDeckId.set(validDecks[0].id);
        }
      },
      error: (err) => {
        this.error.set('Error al cargar los mazos');
        this.notification.show('No se pudieron cargar los mazos', 'error');
      },
    });
  }

  onSubmit(): void {
    const name = this.playerName();
    const deckId = this.selectedDeckId();
    if (!name || !deckId) {
      return;
    }

    const deck = this.decks().find(d => d.id === deckId);
    const deckName = deck?.name ?? '';

    this.loading.set(true);
    this.error.set(null);

    this.matchFacade.createMatch(name, deckId, deckName).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.created.emit(response);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.message ?? 'Error al crear la partida');
        this.notification.show(this.error()!, 'error');
      },
    });
  }
}
