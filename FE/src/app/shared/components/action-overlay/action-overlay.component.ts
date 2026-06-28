import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';

const COLOR_MAP: Record<string, { border: string; bg: string; text: string; bgLight: string }> = {
  red:    { border: '#ef4444', bg: '#dc2626', text: '#fca5a5', bgLight: '#7f1d1d' },
  blue:   { border: '#3b82f6', bg: '#2563eb', text: '#93c5fd', bgLight: '#1e3a5f' },
  green:  { border: '#22c55e', bg: '#16a34a', text: '#86efac', bgLight: '#14532d' },
  cyan:   { border: '#06b6d4', bg: '#0891b2', text: '#67e8f9', bgLight: '#164e63' },
  yellow: { border: '#eab308', bg: '#ca8a04', text: '#fde047', bgLight: '#713f12' },
  orange: { border: '#f97316', bg: '#ea580c', text: '#fdba74', bgLight: '#7c2d12' },
  purple: { border: '#a855f7', bg: '#9333ea', text: '#c084fc', bgLight: '#581c87' },
  teal:   { border: '#14b8a6', bg: '#0d9488', text: '#5eead4', bgLight: '#134e4a' },
  amber:  { border: '#f59e0b', bg: '#d97706', text: '#fcd34d', bgLight: '#78350f' },
};

export interface OverlayItem {
  id: string;
  label: string;
  subtitle?: string;
  disabled?: boolean;
}

export type ActionOverlayMode = 'bar' | 'modal';
export type ActionOverlayLayout = 'custom' | 'info' | 'confirm' | 'selection';
export type ActionOverlayOrientation = 'horizontal' | 'vertical';

@Component({
  selector: 'app-action-overlay',
  imports: [NgTemplateOutlet],
  template: `
    @if (show()) {
      @if (mode() === 'modal') {
        <div class="fixed inset-0 z-[200] bg-black/40 flex items-center justify-center">
          <div class="bg-slate-800 border-t-2 rounded-xl p-6 max-w-lg w-full mx-4 shadow-2xl animate-fade-in"
               [style.borderTopColor]="colors().border">
            @if (layout() === 'custom') {
              <ng-content />
            } @else {
              <ng-container *ngTemplateOutlet="content" />
            }
          </div>
        </div>
      } @else {
        <div class="fixed bottom-0 left-0 right-0 z-[160] bg-slate-800 border-t-2 px-6 py-3 shadow-lg animate-slide-up"
             [style.borderTopColor]="colors().border"
             [class.flex-row]="layout() !== 'selection'"
             [class.flex-col]="layout() === 'selection'"
             [class.items-center]="layout() !== 'selection'"
             [class.items-stretch]="layout() === 'selection'"
             [class.gap-4]="layout() !== 'selection'"
             [class.gap-3]="layout() === 'selection'">
          @if (layout() === 'custom') {
            <ng-content />
          } @else {
            <ng-container *ngTemplateOutlet="content" />
          }
        </div>
      }
    }

    <ng-template #content>
      @if (layout() === 'custom') {
        <!-- content is projected via ng-content -->
      } @else if (layout() === 'info') {
        @if (orientation() === 'vertical') {
          <div class="flex flex-col items-center gap-4 text-center w-full">
            <span class="text-slate-200 text-[0.9375rem] font-semibold">{{ message() }}</span>
            @if (hint()) {
              <span class="text-slate-400 text-xs">{{ hint() }}</span>
            }
            <button (click)="confirm.emit()"
                    class="px-5 py-2 text-sm font-bold rounded text-white cursor-pointer border-none hover:opacity-90 transition-opacity"
                    [style.background]="colors().bg">
              {{ confirmText() }}
            </button>
          </div>
        } @else {
          <div class="flex items-center gap-4 flex-1">
            @if (icon()) {
              <div class="w-8 h-8 rounded-full flex items-center justify-center text-base font-black border-2"
                   [style.borderColor]="colors().border" [style.color]="colors().text">
                {{ icon() }}
              </div>
            }
            <span class="text-slate-200 text-[0.9375rem] font-semibold">{{ message() }}</span>
            @if (hint()) {
              <span class="text-slate-400 text-xs shrink-0">{{ hint() }}</span>
            }
          </div>
        }
      } @else if (layout() === 'confirm') {
        <div class="flex items-center justify-between gap-4 w-full">
          <span class="text-slate-200 text-[0.9375rem]">{{ message() }}</span>
          <div class="flex gap-2 shrink-0">
            @if (cancelText()) {
              <button (click)="cancel.emit()"
                      class="px-5 py-2 text-sm font-bold rounded bg-slate-600 text-white hover:opacity-90 cursor-pointer border-none transition-opacity">
                {{ cancelText() }}
              </button>
            }
            <button [disabled]="confirmDisabled()" (click)="confirm.emit()"
                    class="px-5 py-2 text-sm font-bold rounded text-white cursor-pointer border-none transition-opacity disabled:opacity-50 hover:opacity-90"
                    [style.background]="colors().bg">
              {{ confirmText() }}
            </button>
          </div>
        </div>
      } @else if (layout() === 'selection') {
        <div class="flex flex-col items-center gap-3 w-full">
          @if (title()) {
            <span class="text-slate-200 text-sm font-bold">{{ title() }}</span>
          }
          @if (showCount() && maxSelect() > 0) {
            <span class="text-slate-400 text-xs">{{ selection().length }}/{{ maxSelect() }} seleccionados</span>
          }
          <div class="flex gap-3 flex-wrap justify-center">
            @for (item of items(); track item.id) {
              <button [disabled]="item.disabled"
                      [style.background]="selection().includes(item.id) ? colors().bgLight : undefined"
                      [style.borderColor]="selection().includes(item.id) ? colors().border : undefined"
                      class="px-3 py-2 rounded text-xs font-bold cursor-pointer border-2 transition-all bg-slate-700 border-slate-500"
                      (click)="itemClick.emit(item.id)">
                {{ item.label }}
                @if (item.subtitle) {
                  <br><span class="text-[10px] opacity-75">{{ item.subtitle }}</span>
                }
              </button>
            }
          </div>
          <div class="flex gap-2">
            @if (cancelText()) {
              <button (click)="cancel.emit()"
                      class="px-4 py-1.5 text-sm font-bold rounded bg-slate-600 text-white cursor-pointer border-none">
                {{ cancelText() }}
              </button>
            }
            <button [disabled]="confirmDisabled()" (click)="confirm.emit()"
                    class="px-4 py-1.5 text-sm font-bold rounded text-white cursor-pointer border-none disabled:opacity-50"
                    [style.background]="colors().bg">
              {{ confirmText() }}
            </button>
          </div>
        </div>
      }
    </ng-template>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActionOverlayComponent {
  readonly show = input(false);
  readonly mode = input<ActionOverlayMode>('bar');
  readonly layout = input<ActionOverlayLayout>('custom');
  readonly color = input('blue');

  readonly orientation = input<ActionOverlayOrientation>('horizontal');
  readonly title = input('');
  readonly message = input('');
  readonly hint = input('');
  readonly icon = input('');

  readonly items = input<OverlayItem[]>([]);
  readonly selection = input<string[]>([]);
  readonly maxSelect = input(0);
  readonly showCount = input(false);

  readonly confirmText = input('Confirmar');
  readonly cancelText = input('Cancelar');
  readonly confirmDisabled = input(false);

  readonly confirm = output<void>();
  readonly cancel = output<void>();
  readonly itemClick = output<string>();

  protected readonly colors = computed(() => COLOR_MAP[this.color()] ?? COLOR_MAP['blue']);
}
