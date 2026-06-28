import { ChangeDetectionStrategy, Component, inject, signal, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BackButtonComponent } from '../../../../shared/components/back-button/back-button.component';
import { tap, catchError, of, filter, map, Subscription } from 'rxjs';
import { DeckApiService } from '../../../../core/api/deck-api.service';
import { AuthService } from '../../../../core/services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { DeckBuilderFacadeService } from '../../services/deck-builder-facade.service';
import { DeckValidationModel } from '../../../../shared/models/deck.models';
import { DeckSearchComponent } from './deck-search.component';
import { DeckCardListComponent } from './deck-card-list.component';
import { DeckSummaryComponent } from './deck-summary.component';
import { CardPreviewOverlayComponent } from '../../../match/components/card-preview-overlay/card-preview-overlay.component';

@Component({
  selector: 'app-deck-builder-page',
  imports: [DeckSearchComponent, DeckCardListComponent, DeckSummaryComponent, BackButtonComponent, CardPreviewOverlayComponent],
  template: `
    <div class="relative mx-auto min-h-dvh max-w-6xl bg-[var(--pk-bg)] px-4 py-8">
      <app-back-button />

      <div class="mb-6 flex justify-center">
        <div class="w-full max-w-md rounded-lg border border-[var(--pk-btn-border)] bg-[var(--pk-surface)] p-4">
          <h2 class="mb-3 text-sm font-semibold uppercase tracking-wide text-[var(--pk-text-dim)]">NOMBRE DE MAZO</h2>
          <input
            #nameInput
            type="text"
            placeholder="Nombre del mazo"
            (input)="deckName.set(nameInput.value)"
            [value]="deckName()"
            class="w-full rounded-md border border-[var(--pk-btn-border)] bg-[var(--pk-bg)] px-3 py-2 text-[length:var(--pk-fz-base)] text-[var(--pk-text)] placeholder-[var(--pk-text-dim)] focus:border-[var(--pk-accent)] focus:outline-none focus:ring-1 focus:ring-[var(--pk-accent)]"
          />
        </div>
      </div>

      <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div class="rounded-lg border border-[var(--pk-btn-border)] bg-[var(--pk-surface)] p-4">
          <h2 class="mb-3 text-sm font-semibold uppercase tracking-wide text-[var(--pk-text-dim)]">Buscar cartas</h2>
          <app-deck-search (cardSelected)="onCardSelected($event)" />
        </div>

        <div class="flex flex-col gap-4">
          <div class="rounded-lg border border-[var(--pk-btn-border)] bg-[var(--pk-surface)] p-4">
            <h2 class="mb-3 text-sm font-semibold uppercase tracking-wide text-[var(--pk-text-dim)]">Mazo</h2>
            <app-deck-card-list
              [cards]="facade.cards()"
              (increment)="onIncrement($event)"
              (decrement)="onDecrement($event)"
              (cardDropped)="onCardSelected($event)"
            />
          </div>

          <app-deck-summary
            [totalCards]="facade.totalCards()"
            [pokemonCount]="facade.pokemonCount()"
            [trainerCount]="facade.trainerCount()"
            [energyCount]="facade.energyCount()"
            [basicPokemonCount]="facade.basicPokemonCount()"
            [validation]="validationResult()"
          />

          <div class="flex gap-2">
            <button
              class="pk-accent-btn flex-1"
              style="font-size: var(--pk-fz-base)"
              [disabled]="!deckName().trim() || facade.isEmpty() || saving()"
              (click)="onSave()"
            >
              Guardar
            </button>
            <button
              class="pk-btn"
              (click)="onRandom()"
            >
              Al Azar
            </button>
            <button
              class="pk-btn"
              (click)="onClear()"
            >
              Limpiar
            </button>
          </div>
        </div>
      </div>
      <app-card-preview-overlay />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckBuilderPage implements OnDestroy {
  protected readonly facade = inject(DeckBuilderFacadeService);
  private readonly deckApi = inject(DeckApiService);
  private readonly authService = inject(AuthService);
  protected readonly notificationService = inject(NotificationService);
  protected readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly deckName = signal('');
  protected readonly validationResult = signal<DeckValidationModel | null>(null);
  protected readonly saving = signal(false);

  private deckId: string | null = null;
  private playerId: string | null = null;
  private subscription: Subscription | null = null;

  constructor() {
    this.subscription = this.route.paramMap.pipe(
      map((params) => params.get('id')),
      filter((id): id is string => id !== null),
      tap((id) => {
        this.deckId = id;
        this.loadDeck(id);
      }),
    ).subscribe();
    this.route.queryParamMap.pipe(
      map((params) => params.get('playerId')),
      tap((id) => { this.playerId = id; }),
    ).subscribe();
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  private loadDeck(id: string): void {
    this.deckApi.get(id).pipe(
      tap((deck) => {
        this.deckName.set(deck.name);
        this.facade.setCards(
          deck.cards.map((c) => ({
            cardId: c.cardId,
            name: c.name,
            supertype: c.supertype,
            subtypes: c.subtypes ?? [],
            stage: c.stage,
            isBasicEnergy: c.isBasicEnergy,
            quantity: c.quantity,
          })),
        );
      }),
      catchError(() => {
        this.notificationService.show('Error al cargar el mazo', 'error');
        return of(null);
      }),
    ).subscribe();
  }

  private isEditMode(): boolean {
    return this.deckId !== null;
  }

  onCardSelected(event: { cardId: string; name: string; supertype: string; subtypes: string[]; stage?: string }): void {
    if (event.supertype !== 'ENERGY') {
      const existing = this.facade.cards().find((c) => c.cardId === event.cardId);
      if (existing && existing.quantity >= 4) {
        this.notificationService.show('Máximo 4 copias de esta carta', 'warning');
        return;
      }
    }
    if (event.subtypes.includes('ACE_SPEC') && this.facade.aceSpecCount() >= 1) {
      this.notificationService.show('Solo se permite 1 carta ACE SPEC por mazo', 'warning');
      return;
    }
    this.facade.addCard(event.cardId, event.name, event.supertype, event.subtypes, event.stage);
    this.validationResult.set(null);
  }

  onIncrement(cardId: string): void {
    const entry = this.facade.cards().find((c) => c.cardId === cardId);
    if (entry) {
      if (entry.subtypes.includes('ACE_SPEC') && this.facade.aceSpecCount() >= 1) {
        this.notificationService.show('Solo se permite 1 carta ACE SPEC por mazo', 'warning');
        return;
      }
      this.facade.addCard(cardId, entry.name, entry.supertype, entry.subtypes, entry.stage, entry.isBasicEnergy);
    }
    this.validationResult.set(null);
  }

  onDecrement(cardId: string): void {
    this.facade.removeCard(cardId);
    this.validationResult.set(null);
  }

  onClear(): void {
    this.facade.reset();
    this.deckName.set('');
    this.validationResult.set(null);
  }

  onRandom(): void {
    this.deckApi.generateRandom().subscribe({
      next: (deck) => {
        this.facade.setCards(
          deck.cards.map((c) => ({
            cardId: c.cardId,
            name: c.name,
            supertype: c.supertype,
            subtypes: c.subtypes ?? [],
            stage: c.stage,
            isBasicEnergy: c.isBasicEnergy,
            quantity: c.quantity,
          })),
        );
        this.deckName.set(deck.name);
        this.validationResult.set(deck.valid
          ? { valid: true, errors: [] }
          : { valid: false, errors: deck.validation?.errors ?? [] },
        );
        this.notificationService.show(
          deck.valid ? 'Mazo aleatorio generado' : 'Mazo aleatorio generado con errores',
          deck.valid ? 'success' : 'warning',
        );
      },
      error: () => this.notificationService.show('Error al generar mazo aleatorio', 'error'),
    });
  }
  onSave(): void {
    const name = this.deckName().trim();
    if (!name || this.facade.isEmpty()) return;

    this.saving.set(true);

    const validateObs = this.isEditMode()
      ? this.deckApi.validate(this.deckId!)
      : this.facade.validate();

    validateObs.subscribe({
      next: (result) => {
        this.validationResult.set(result);
        if (!result.valid) {
          this.saving.set(false);
          this.notificationService.show('Corregí los errores antes de guardar', 'warning');
          return;
        }

        const cards = this.facade.cards().map((c) => ({ cardId: c.cardId, quantity: c.quantity }));

        const saveObs = this.isEditMode()
          ? this.deckApi.update(this.deckId!, { name, cards })
          : this.deckApi.create({ name, playerId: this.playerId ?? this.authService.playerId() ?? '', cards });

        saveObs.subscribe({
          next: () => {
            this.facade.reset();
            this.notificationService.show(
              this.isEditMode() ? 'Mazo actualizado' : 'Mazo creado',
              'success',
            );
            this.router.navigate(['/decks'], { replaceUrl: true });
          },
          error: () => {
            this.saving.set(false);
            this.notificationService.show('Error al guardar el mazo', 'error');
          },
        });
      },
      error: () => {
        this.saving.set(false);
        this.notificationService.show('Error al validar', 'error');
      },
    });
  }
}
