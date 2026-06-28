import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { CardSummaryResponse } from '../../models/card.models';
import { CardImagePipe } from '../../pipes/card-image.pipe';
import { CardPreviewService } from '../../../features/match/services/card-preview.service';

@Component({
  selector: 'app-card-view',
  imports: [CardImagePipe],
  template: `
    <div class="rounded-lg border bg-white shadow-sm transition-shadow hover:shadow-md">
      <div class="aspect-[2.5/3.5] bg-gray-100 rounded-t-lg relative group transition-transform duration-200 group-hover:scale-[1.35]">
        <img
          [src]="card().id | cardImage:'small'"
          [alt]="card().name"
          class="h-full w-full object-contain"
          (error)="onImageError($event)"
        />
        <button
          class="absolute top-1 right-1 w-6 h-6 flex items-center justify-center rounded-full bg-black/60 text-white text-xs opacity-0 group-hover:opacity-100 transition-opacity duration-150 cursor-pointer hover:bg-black/80 z-10 border-none leading-none shadow-lg"
          (mousedown)="$event.stopPropagation()"
          (click)="$event.stopPropagation(); previewSvc.open({ cardId: card().id, name: card().name })"
          title="Ver detalle"
        >+</button>
      </div>
      <div class="p-2">
        <p class="truncate text-sm font-medium">{{ card().name }}</p>
        <p class="text-xs text-gray-500">{{ card().supertype }} · {{ card().setCode }}</p>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardViewComponent {
  protected readonly previewSvc = inject(CardPreviewService);

  card = input.required<CardSummaryResponse>();

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
    const parent = img.parentElement;
    if (parent) {
      parent.classList.add('flex', 'items-center', 'justify-center');
      const placeholder = document.createElement('div');
      placeholder.className = 'text-center text-sm text-gray-400';
      placeholder.textContent = this.card().name;
      parent.appendChild(placeholder);
    }
  }
}
