import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { CardImagePipe } from '../../../../shared/pipes/card-image.pipe';
import { EnergyIconPipe } from '../../../../shared/pipes/energy-icon.pipe';

export interface EnergyOption {
  instanceId: string;
  cardId: string;
}

@Component({
  selector: 'app-energy-selector-dialog',
  imports: [CardImagePipe],
  template: `
    <div class="dialog-overlay" (click)="onCancel()">
      <div class="dialog-content" (click)="$event.stopPropagation()">
        <h3>Seleccionar energías para descartar</h3>
        <p class="dialog-hint">Elegí {{ count() }} energía(s) para descartar del Pokémon rival.</p>
        <div class="energy-list">
          @for (e of energies(); track e.instanceId) {
            <label class="energy-item" [class.selected]="selectedIds().has(e.instanceId)">
              <input
                type="checkbox"
                [checked]="selectedIds().has(e.instanceId)"
                [disabled]="!selectedIds().has(e.instanceId) && selectedIds().size >= count()"
                (change)="toggleEnergy(e.instanceId)"
              />
              <img [src]="e.cardId | cardImage" alt="Energy" class="energy-img" />
            </label>
          }
        </div>
        <div class="dialog-actions">
          <button class="btn btn-cancel" (click)="onCancel()">Cancelar</button>
          <button
            class="btn btn-confirm"
            [disabled]="selectedIds().size !== count()"
            (click)="onConfirm()"
          >Confirmar ({{ selectedIds().size }}/{{ count() }})</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dialog-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,0.6);
      display: flex; align-items: center; justify-content: center; z-index: 1000;
    }
    .dialog-content {
      background: #1e293b; border: 1px solid #334155; border-radius: 0.5rem;
      padding: 1.5rem; max-width: 400px; width: 90%;
    }
    h3 { margin: 0 0 0.5rem; color: #f1f5f9; font-size: 1rem; }
    .dialog-hint { margin: 0 0 1rem; color: #94a3b8; font-size: 0.8125rem; }
    .energy-list { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 1rem; }
    .energy-item {
      border: 2px solid #475569; border-radius: 0.375rem; padding: 0.25rem;
      cursor: pointer; transition: border-color 0.15s;
    }
    .energy-item.selected { border-color: #fbbf24; }
    .energy-item input { display: none; }
    .energy-img { width: 50px; height: auto; display: block; }
    .dialog-actions { display: flex; gap: 0.5rem; justify-content: flex-end; }
    .btn {
      padding: 0.5rem 1rem; border-radius: 0.375rem; border: 1px solid #475569;
      font-size: 0.8125rem; font-weight: 600; cursor: pointer; font-family: inherit;
    }
    .btn:disabled { opacity: 0.4; cursor: not-allowed; }
    .btn-cancel { background: transparent; color: #ef4444; border-color: #ef4444; }
    .btn-confirm { background: #2563eb; color: #fff; border-color: #2563eb; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EnergySelectorDialogComponent {
  readonly pokemonInstanceId = input.required<string>();
  readonly energies = input.required<EnergyOption[]>();
  readonly count = input.required<number>();

  readonly confirmed = output<string[]>();
  readonly cancelled = output<void>();

  private readonly _selectedIds = signal<Set<string>>(new Set());
  protected readonly selectedIds = this._selectedIds.asReadonly();

  protected toggleEnergy(instanceId: string): void {
    this._selectedIds.update(set => {
      const next = new Set(set);
      if (next.has(instanceId)) {
        next.delete(instanceId);
      } else if (next.size < this.count()) {
        next.add(instanceId);
      }
      return next;
    });
  }

  protected onConfirm(): void {
    this.confirmed.emit(Array.from(this._selectedIds()));
  }

  protected onCancel(): void {
    this.cancelled.emit();
  }
}
