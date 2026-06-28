import { ChangeDetectionStrategy, Component, computed, effect, ElementRef, input, signal, viewChild } from '@angular/core';
import { GameEventDto } from '../../../../shared/models/game-action.models';
import { formatGameEvent, LogEntry } from '../../utils/game-event-formatter';

interface DisplayEntry {
  type: 'event' | 'turn-separator';
  message?: string;
  cssClass?: string;
  turnNumber?: number;
}

@Component({
  selector: 'app-game-log',
  host: {
    '(document:keydown.escape)': 'closeDrawer()',
  },
  template: `
    <!-- Backdrop — solo cuando el drawer está abierto -->
    @if (isOpen()) {
      <div
        class="fixed inset-0 z-40 bg-black/40"
        (click)="closeDrawer()"
      ></div>
    }

    <!-- Toggle button — siempre visible en el borde derecho -->
    <button
      (click)="toggleDrawer()"
      class="fixed right-0 top-1/2 -translate-y-1/2 z-50 flex items-center gap-2 px-2 py-3 rounded-l-lg bg-slate-800 border border-r-0 border-slate-700 text-xs font-bold uppercase tracking-wider text-slate-400 cursor-pointer select-none hover:bg-slate-700 transition-colors shadow-lg [writing-mode:vertical-lr]"
    >
      @if (events().length === 0) {
        Eventos
      } @else {
        Eventos ({{ events().length }})
      }
    </button>

    <!-- Drawer panel — se desliza desde la derecha -->
    <div
      class="fixed right-0 top-0 h-full z-50 w-80 bg-slate-900 border-l border-slate-800 shadow-2xl transition-transform duration-300 ease-in-out"
      [class.translate-x-0]="isOpen()"
      [class.translate-x-full]="!isOpen()"
    >
      <!-- Header del drawer -->
      <div class="flex items-center justify-between px-4 py-3 bg-slate-800 border-b border-slate-700">
        <span class="text-xs font-bold uppercase tracking-wider text-slate-400">
          @if (events().length === 0) {
            Eventos
          } @else {
            Eventos ({{ events().length }})
          }
        </span>
        <button
          (click)="closeDrawer()"
          class="text-slate-400 hover:text-slate-200 cursor-pointer transition-colors text-lg leading-none"
        >✕</button>
      </div>

      <!-- Lista de eventos — scroll vertical -->
      <div
        class="overflow-y-auto max-h-[calc(100vh-3rem)] text-slate-300 py-1 text-sm"
        #logContainer
      >
        @if (displayEntries().length === 0) {
          <p class="text-slate-400 text-center py-6 px-4 m-0 text-sm">No hay eventos aún</p>
        } @else {
          @for (entry of displayEntries(); track $index) {
            @if (entry.type === 'turn-separator') {
              <!-- Separador de turno -->
              <div class="flex items-center gap-2 px-4 py-2 bg-slate-800/80 border-b border-slate-700">
                <div class="h-px flex-1 bg-slate-600"></div>
                <span class="text-[0.65rem] font-bold uppercase tracking-widest text-slate-400 shrink-0">
                  Turno {{ entry.turnNumber }}
                </span>
                <div class="h-px flex-1 bg-slate-600"></div>
              </div>
            } @else {
              <!-- Entrada de evento normal -->
              <div
                class="flex items-start gap-2 px-4 py-2 border-b border-slate-700/50 leading-relaxed last:border-b-0 border-l-2"
                [class.border-red-500]="entry.cssClass === 'hostile'"
                [class.border-amber-400]="entry.cssClass === 'reward'"
                [class.border-emerald-400]="entry.cssClass === 'heal'"
                [class.border-blue-400]="entry.cssClass === 'energy'"
                [class.border-orange-400]="entry.cssClass === 'attack'"
                [class.border-violet-400]="entry.cssClass === 'evo'"
                [class.border-yellow-400]="entry.cssClass === 'status'"
                [class.border-cyan-400]="entry.cssClass === 'phase'"
                [class.border-amber-500]="entry.cssClass === 'mulligan'"
                [class.border-indigo-400]="entry.cssClass === 'setup'"
                [class.border-slate-500]="!entry.cssClass || entry.cssClass === 'info'"
                [class.bg-red-500/10]="entry.cssClass === 'hostile'"
                [class.bg-amber-400/10]="entry.cssClass === 'reward'"
                [class.bg-emerald-500/10]="entry.cssClass === 'heal'"
                [class.bg-blue-400/10]="entry.cssClass === 'energy'"
                [class.bg-orange-500/10]="entry.cssClass === 'attack'"
                [class.bg-violet-500/10]="entry.cssClass === 'evo'"
                [class.bg-yellow-500/10]="entry.cssClass === 'status'"
                [class.bg-cyan-500/10]="entry.cssClass === 'phase'"
                [class.bg-amber-500/10]="entry.cssClass === 'mulligan'"
                [class.bg-indigo-500/10]="entry.cssClass === 'setup'"
              >
                <span class="flex-1 min-w-0 break-words">{{ entry.message }}</span>
              </div>
            }
          }
        }
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameLogComponent {
  readonly events = input<GameEventDto[]>([]);
  readonly myPlayerId = input<string | null>(null);
  private readonly logContainer = viewChild<ElementRef<HTMLElement>>('logContainer');

  /** Controla si el drawer está abierto o cerrado */
  readonly isOpen = signal(false);

  /** Transforma los eventos crudos del backend en entradas listas para renderizar,
   *  insertando separadores de turno cuando el turnNumber cambia */
  readonly displayEntries = computed<DisplayEntry[]>(() => {
    const raw = this.events();
    const myId = this.myPlayerId();
    const result: DisplayEntry[] = [];
    let lastTurn: number | undefined;
    for (const event of raw) {
      const turn = event.turnNumber;
      // Insertar separador si cambia el turno
      if (turn != null && lastTurn != null && turn !== lastTurn) {
        result.push({ type: 'turn-separator', turnNumber: turn });
      }
      const entry = formatGameEvent(event, myId);
      result.push({ type: 'event', ...entry });
      // Si es el primer evento con turnNumber, registrar el turno inicial
      if (turn != null && lastTurn == null) {
        lastTurn = turn;
      }
      lastTurn = turn ?? lastTurn;
    }
    return result;
  });

  constructor() {
    effect(() => {
      this.events();
      const el = this.logContainer()?.nativeElement;
      if (el) {
        requestAnimationFrame(() => {
          el.scrollTop = el.scrollHeight;
        });
      }
    });
  }

  /** Abre/cierra el drawer */
  protected toggleDrawer(): void {
    this.isOpen.update(v => !v);
  }

  /** Cierra el drawer — también vinculado a Escape vía host binding */
  protected closeDrawer(): void {
    if (this.isOpen()) {
      this.isOpen.set(false);
    }
  }
}
