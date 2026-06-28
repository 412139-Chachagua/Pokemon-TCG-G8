import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BackButtonComponent } from '../../../../shared/components/back-button/back-button.component';
import { CardRepositoryService } from '../../../../core/services/card-repository.service';
import { CardDetailResponse } from '../../../../shared/models/card.models';
import { PokemonCardComponent } from '../../../../shared/components/pokemon-card/pokemon-card.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';
import { ButtonComponent } from '../../../../shared/components/button/button.component';

@Component({
  selector: 'app-card-detail-page',
  imports: [BackButtonComponent, PokemonCardComponent, LoadingSpinnerComponent, ButtonComponent],
  template: `
    <div class="relative min-h-dvh">
      <app-back-button />
      <main class="min-h-dvh bg-[var(--pk-bg)] p-8 max-w-4xl mx-auto pt-20">

      @if (loading()) {
        <app-loading-spinner />
      } @else if (error()) {
        <div class="flex flex-col items-center gap-4 py-12">
          <p class="text-red-600">{{ error() }}</p>
          <app-button variant="primary" (click)="loadCard()">Reintentar</app-button>
        </div>
      } @else {
        @let c = card();
        @if (c) {
          <app-pokemon-card [card]="c" />
        } @else {
          <div class="py-12 text-center">
            <p class="text-gray-500">Carta no encontrada</p>
          </div>
        }
      }
    </main>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly cardRepository = inject(CardRepositoryService);

  protected readonly card = signal<CardDetailResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.loadCard();
  }

  protected loadCard(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('Carta no encontrada');
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.cardRepository.resolve(id).then(
      (card) => {
        this.card.set(card);
        this.loading.set(false);
      },
      () => {
        this.error.set('Error al cargar la carta');
        this.loading.set(false);
      },
    );
  }
}
