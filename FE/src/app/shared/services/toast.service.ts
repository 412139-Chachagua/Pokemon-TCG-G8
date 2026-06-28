import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: string;
  text: string;
  type: 'hostile' | 'reward' | 'heal' | 'energy' | 'info';
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly _toasts = signal<ToastMessage[]>([]);
  readonly toasts = this._toasts.asReadonly();

  show(text: string, type: ToastMessage['type'] = 'info', durationMs = 3000): void {
    const id = crypto.randomUUID();
    this._toasts.update(list => [...list, { id, text, type }]);
    setTimeout(() => {
      this._toasts.update(list => list.filter(t => t.id !== id));
    }, durationMs);
  }

  clear(): void {
    this._toasts.set([]);
  }
}
