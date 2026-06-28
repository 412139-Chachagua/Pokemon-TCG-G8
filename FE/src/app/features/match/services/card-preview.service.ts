import { Injectable, signal } from '@angular/core';

export interface CardPreviewData {
  cardId: string;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class CardPreviewService {
  private readonly _previewCard = signal<CardPreviewData | null>(null);
  readonly previewCard = this._previewCard.asReadonly();

  open(data: CardPreviewData): void {
    this._previewCard.set(data);
  }

  close(): void {
    this._previewCard.set(null);
  }
}
