import { ChangeDetectionStrategy, Component, HostListener, inject } from '@angular/core';
import { CardImagePipe } from '../../../../shared/pipes/card-image.pipe';
import { CardPreviewService } from '../../services/card-preview.service';

@Component({
  selector: 'app-card-preview-overlay',
  imports: [CardImagePipe],
  template: `
    @if (previewSvc.previewCard(); as card) {
      <div
        class="fixed inset-0 z-[300] flex items-center justify-center bg-black/70 p-4"
        (click)="onBackdropClick($event)"
      >
        <div class="relative max-h-[90vh] max-w-[90vw] flex items-center justify-center">
          <button
            class="absolute -top-3 -right-3 z-10 w-8 h-8 flex items-center justify-center rounded-full bg-[var(--pk-dark)] text-white text-lg font-bold cursor-pointer border-2 border-white/30 hover:bg-red-600 transition-colors shadow-lg"
            (click)="previewSvc.close()"
          >
            ✕
          </button>
          <img
            [src]="card.cardId | cardImage:'large'"
            [alt]="card.name"
            class="max-h-[85vh] max-w-[85vw] w-auto h-auto object-contain rounded-lg shadow-2xl"
          />
        </div>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardPreviewOverlayComponent {
  protected readonly previewSvc = inject(CardPreviewService);

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.previewSvc.previewCard()) {
      this.previewSvc.close();
    }
  }

  protected onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('fixed')) {
      this.previewSvc.close();
    }
  }
}
