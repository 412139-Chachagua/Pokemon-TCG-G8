import { ChangeDetectionStrategy, Component, signal } from '@angular/core';

interface EnergyLegendEntry {
  type: string;
  color: string;
  name: string;
}

const ENERGY_LEGEND: EnergyLegendEntry[] = [
  { type: 'FIRE', color: '#ef4444', name: 'Fuego' },
  { type: 'WATER', color: '#3b82f6', name: 'Agua' },
  { type: 'GRASS', color: '#22c55e', name: 'Planta' },
  { type: 'LIGHTNING', color: '#eab308', name: 'Rayo' },
  { type: 'PSYCHIC', color: '#a855f7', name: 'Psíquico' },
  { type: 'FIGHTING', color: '#d97706', name: 'Lucha' },
  { type: 'DARKNESS', color: '#4a044e', name: 'Siniestro' },
  { type: 'METAL', color: '#9ca3af', name: 'Metal' },
  { type: 'FAIRY', color: '#f472b6', name: 'Hada' },
  { type: 'DRAGON', color: '#f59e0b', name: 'Dragón' },
  { type: 'COLORLESS', color: '#d1d5db', name: 'Incolora' },
];

@Component({
  selector: 'app-legend-panel',
  host: {
    '(document:keydown.escape)': 'onEscape()',
  },
  template: `
    <button
      (click)="toggle()"
      class="fixed top-0 left-1/2 -translate-x-1/2 z-50 flex items-center gap-1.5 px-3 py-1.5 rounded-b-lg bg-slate-800 border border-t-0 border-slate-700 text-xs font-bold tracking-wider text-slate-400 cursor-pointer select-none hover:bg-slate-700 transition-colors shadow-lg"
    >
      Leyenda
    </button>

    <div
      class="fixed top-0 left-1/2 -translate-x-1/2 z-40 w-[420px] max-w-[90vw] transition-transform duration-300 ease-in-out pointer-events-none"
      [class.translate-y-0]="isOpen()"
      [class.-translate-y-full]="!isOpen()"
    >
      <div class="bg-slate-900 border border-t-0 border-slate-700 rounded-b-xl shadow-2xl overflow-hidden">
        <div class="flex items-center justify-end px-4 py-2 bg-slate-800 border-b border-slate-700">
          <button
            (click)="close()"
            class="pointer-events-auto text-slate-400 hover:text-slate-200 cursor-pointer transition-colors text-lg leading-none"
          >✕</button>
        </div>

        <div class="p-3 pointer-events-none">
          <p class="text-[0.625rem] text-slate-500 mb-3 leading-relaxed">
            Estos colores indican el tipo de energía en los bordes de las cartas en tu mano y las fichas de energía adheridas a los Pokémon.
          </p>

          <div class="grid grid-cols-2 gap-1.5">
            @for (entry of entries; track entry.type) {
              <div class="flex items-center gap-2 px-2 py-1.5 rounded bg-slate-800/50">
                <span
                  class="w-3.5 h-3.5 rounded-full flex-shrink-0"
                  [style.background-color]="entry.color"
                ></span>
                <span class="text-[0.6875rem] font-medium text-slate-300">{{ entry.name }}</span>
              </div>
            }
          </div>
        </div>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LegendPanelComponent {
  readonly isOpen = signal(false);

  protected readonly entries = ENERGY_LEGEND;

  protected toggle(): void {
    this.isOpen.update(v => !v);
  }

  protected close(): void {
    if (this.isOpen()) {
      this.isOpen.set(false);
    }
  }

  protected onEscape(): void {
    this.close();
  }
}
