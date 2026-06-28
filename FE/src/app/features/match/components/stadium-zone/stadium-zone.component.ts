import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { CardImagePipe } from '../../../../shared/pipes/card-image.pipe';
import { CardRepositoryService } from '../../../../core/services/card-repository.service';
import { CardPreviewService } from '../../services/card-preview.service';

@Component({
  selector: 'app-stadium-zone',
  standalone: true,
  imports: [CardImagePipe],
  template: `
    @if (stadiumCardDefId(); as cardDefId) {
      <div class="flex flex-col items-center gap-1 p-2 rounded-lg border border-[var(--pk-btn-border)] bg-[var(--pk-surface)]/40 backdrop-blur-sm min-w-[80px] group">
        <span class="text-[10px] text-[var(--pk-text-dim)] font-bold uppercase tracking-wider">Estadio</span>
        <div class="relative hover:scale-[1.8] hover:z-20 transition-[transform] duration-150">
          <img [src]="cardDefId | cardImage" [alt]="stadiumName()" class="w-16 h-22 object-contain rounded" loading="lazy" />
          <button
            class="absolute top-0.5 right-0.5 w-5 h-5 flex items-center justify-center rounded-full bg-black/60 text-white text-xs opacity-0 group-hover:opacity-100 transition-opacity duration-150 cursor-pointer hover:bg-black/80 z-10 border-none leading-none"
            (click)="$event.stopPropagation(); previewSvc.open({ cardId: cardDefId, name: stadiumName() })"
            title="Ver detalle"
          >+</button>
        </div>
        <span class="text-xs text-slate-300 font-semibold text-center leading-tight">{{ stadiumName() }}</span>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StadiumZoneComponent {
  readonly stadiumCardInstanceId = input<string | null>(null);
  readonly stadiumCardDefinitionId = input<string | null>(null);

  private readonly cardRepo = inject(CardRepositoryService);
  protected readonly previewSvc = inject(CardPreviewService);

  readonly stadiumCardDefId = computed(() => this.stadiumCardDefinitionId());

  readonly stadiumName = computed(() => {
    const defId = this.stadiumCardDefinitionId();
    if (!defId) return '';
    const cardDef = this.cardRepo.getFromCache(defId);
    return cardDef?.name ?? '';
  });
}
