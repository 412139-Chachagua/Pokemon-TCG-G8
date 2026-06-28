import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { AuthService } from '../../../../core/services/auth.service';
import { AudioService } from '../../../../core/audio/audio.service';
import { AvatarService } from '../../../../core/services/avatar.service';

interface MenuItem {
  id: string;
  label: string;
  type?: 'action' | 'slider' | 'danger';
}

@Component({
  selector: 'app-match-menu',
  host: {
    '(keydown.arrowdown)': 'onArrowDown($event)',
    '(keydown.arrowup)': 'onArrowUp($event)',
    '(keydown.arrowright)': 'onArrowRight($event)',
    '(keydown.arrowleft)': 'onArrowLeft($event)',
    '(keydown.enter)': 'onEnter($event)',
    '(document:keydown)': 'onKeyDown($event)',
  },
  template: `
    @if (isOpen()) {
      <div class="fixed inset-0 z-[100] flex items-center justify-center bg-black/70" (click)="closeMenu.emit()">
        <div
          class="bg-[var(--pk-surface)] border-2 border-[var(--pk-accent)] rounded-xl shadow-2xl w-[420px] max-w-[92vw] max-h-[85vh] flex flex-col animate-[pk-fade-in_0.2s_ease-out]"
          (click)="$event.stopPropagation()"
        >
          <!-- Header -->
          <div class="flex items-center gap-3 px-5 py-4 border-b border-[var(--pk-dark)] shrink-0">
            <div class="w-10 h-10 rounded-full bg-[var(--pk-panel)] border-2 border-[var(--pk-accent)] flex items-center justify-center overflow-hidden shrink-0">
              @if (avatarUrl(); as url) {
                <img [src]="url" alt="Avatar" class="w-full h-full object-cover" />
              } @else {
                <span class="text-[0.65rem] font-bold text-[var(--pk-text-bright)]">{{ avatarInitials() }}</span>
              }
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-bold text-[var(--pk-text-bright)] truncate">{{ playerName() }}</p>
              <p class="text-[0.65rem] text-[var(--pk-text-dim)]">Menú de partida</p>
            </div>
            <button
              (click)="closeMenu.emit()"
              class="w-8 h-8 flex items-center justify-center rounded-full bg-[var(--pk-dark)] text-[var(--pk-text-dim)] hover:text-[var(--pk-text)] hover:bg-[var(--pk-panel)] cursor-pointer transition-colors border-none"
            >✕</button>
          </div>

          <!-- Menu items -->
          <div class="flex-1 overflow-y-auto p-4 space-y-2.5">
            @for (item of menuItems; track item.id; let i = $index) {
              @if (item.id === 'shortcuts_separator' || item.id === 'concede_separator') {
                <div class="border-t border-[var(--pk-dark)] my-2"></div>
              } @else {
                <div
                  tabindex="0"
                  class="flex items-center gap-3 px-3 py-3 rounded-[var(--pk-radius)] border-2 cursor-pointer transition-all select-none outline-none"
                  [class.border-[var(--pk-accent)]]="selectedIndex() === i"
                  [class.ring-2]="selectedIndex() === i"
                  [class.ring-[var(--pk-accent)]]="selectedIndex() === i"
                  [class.bg-[var(--pk-accent)]/10]="selectedIndex() === i"
                  [class.border-[var(--pk-btn-border)]]="selectedIndex() !== i"
                  [class.bg-[var(--pk-panel)]]="selectedIndex() !== i"
                  [class.hover:border-[var(--pk-accent)]]="selectedIndex() !== i"
                  [class.!border-[var(--pk-error)]]="item.type === 'danger'"
                  [class.!text-[var(--pk-error)]]="item.type === 'danger'"
                  [class.hover:!bg-[var(--pk-error-bg)]]="item.type === 'danger'"
                  (click)="onItemClick(i)"
                  (mouseenter)="selectedIndex.set(i)"
                  (focus)="selectedIndex.set(i)"
                >
                  <span class="shrink-0 w-5 h-5 flex items-center justify-center">
                    @if (item.id === 'resume') {
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
                    } @else if (item.id === 'music') {
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>
                    } @else if (item.id === 'sfx') {
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>
                    } @else if (item.id === 'concede') {
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
                    }
                  </span>
                  <span class="flex-1 text-xs font-semibold text-[var(--pk-text-bright)]" [class.!text-[var(--pk-error)]]="item.type === 'danger'">{{ item.label }}</span>
                  @if (item.type === 'slider') {
                    <span class="text-[0.65rem] text-[var(--pk-text-dim)] w-8 text-right font-semibold">{{ sliderValue(item.id) }}%</span>
                  } @else {
                    <span class="text-[0.5625rem] text-[var(--pk-text-dim)] bg-slate-700/60 px-1.5 py-0.5 rounded font-mono uppercase">{{ itemKeyLabel(item.id) }}</span>
                  }
                </div>
                @if (item.type === 'slider') {
                  <div class="px-3 pb-2 -mt-1">
                    <input
                      type="range" min="0" max="100" step="1"
                      [value]="sliderValue(item.id)"
                      (input)="onSliderChange(item.id, $event)"
                      class="w-full h-2 rounded-full appearance-none cursor-pointer bg-[var(--pk-dark)] accent-[var(--pk-accent)]"
                    />
                  </div>
                }
              }
            }
          </div>

          <!-- Footer -->
          <div class="px-5 py-3 border-t border-[var(--pk-dark)] text-center text-[0.6rem] text-[var(--pk-text-dim)] shrink-0">
            Presioná Escape para cerrar
          </div>
        </div>
      </div>
    }

    @if (showConcedeConfirm()) {
      <div class="fixed inset-0 z-[110] flex items-center justify-center bg-black/70" (click)="cancelConcede()">
        <div class="bg-[var(--pk-surface)] border-2 border-[var(--pk-error)] rounded-xl p-6 max-w-sm w-[90%] shadow-2xl animate-[pk-fade-in_0.2s_ease-out] text-center" (click)="$event.stopPropagation()">
          <div class="flex justify-center mb-3">
            <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--pk-error)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18.36 6.64a9 9 0 1 1-12.73 0"/><line x1="12" y1="2" x2="12" y2="12"/></svg>
          </div>
          <p class="text-sm font-bold text-[var(--pk-text-bright)] mb-1">Abandonar partida</p>
          <p class="text-xs text-[var(--pk-text-dim)] mb-5">¿Estás seguro? Se contará como derrota.</p>
          <div class="flex gap-3 justify-center">
            <button
              class="px-5 py-2.5 rounded-[var(--pk-radius)] border-2 border-[var(--pk-btn-border)] bg-[var(--pk-panel)] text-xs font-semibold text-[var(--pk-text-bright)] cursor-pointer hover:border-[var(--pk-accent)] transition-colors"
              (click)="cancelConcede()"
            >Cancelar</button>
            <button
              class="px-5 py-2.5 rounded-[var(--pk-radius)] border-2 border-[var(--pk-error)] bg-[var(--pk-error-bg)] text-xs font-semibold text-[var(--pk-error)] cursor-pointer hover:bg-[var(--pk-error)]/20 transition-colors"
              (click)="confirmConcede()"
            >Abandonar</button>
          </div>
        </div>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MatchMenuComponent {
  private readonly authService = inject(AuthService);
  protected readonly audioService = inject(AudioService);
  private readonly avatarService = inject(AvatarService);

  readonly isOpen = input(false);
  readonly matchId = input<string>('');
  readonly closeMenu = output<void>();
  readonly concede = output<string>();

  readonly selectedIndex = signal(0);
  readonly showConcedeConfirm = signal(false);

  readonly playerName = computed(() => this.authService.player()?.displayName ?? 'Jugador');
  readonly avatarUrl = computed(() => {
    const avatar = this.authService.player()?.avatarUrl;
    return avatar ? this.avatarService.resolve(avatar) : null;
  });
  readonly avatarInitials = computed(() => {
    const name = this.playerName();
    return name.split(' ').filter(p => p).map(p => p[0].toUpperCase()).slice(0, 2).join('');
  });

  protected readonly menuItems: MenuItem[] = [
    { id: 'resume', label: 'Volver a la partida', type: 'action' },
    { id: 'music', label: 'Música de fondo', type: 'slider' },
    { id: 'sfx', label: 'Efectos de sonido', type: 'slider' },
    { id: 'concede_separator', label: '' },
    { id: 'concede', label: 'Abandonar partida', type: 'danger' },
  ];

  protected itemKeyLabel(id: string): string {
    if (id === 'resume') return 'V';
    if (id === 'concede') return 'S';
    const idx = this.menuItems.findIndex(m => m.id === id);
    return idx >= 0 ? `[${idx + 1}]` : '';
  }

  protected onKeyDown(event: Event): void {
    const ke = event as KeyboardEvent;
    if (!this.isOpen() || this.showConcedeConfirm()) return;
    const key = ke.key.toLowerCase();
    if (key === 'v') {
      ke.preventDefault();
      this.closeMenu.emit();
    } else if (key === 's') {
      ke.preventDefault();
      this.showConcedeConfirm.set(true);
    }
  }

  protected sliderValue(id: string): number {
    if (id === 'music') return this.audioService.boardVolume();
    if (id === 'sfx') return this.audioService.sfxVolume();
    return 0;
  }

  protected onSliderChange(id: string, event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    if (id === 'music') this.audioService.setBoardVolume(value);
    if (id === 'sfx') this.audioService.setSfxVolume(value);
  }

  protected onArrowDown(event: Event): void {
    const ke = event as KeyboardEvent;
    if (!this.isOpen() || this.showConcedeConfirm()) return;
    ke.preventDefault();
    const items = this.navigableItems();
    const current = this.selectedIndex();
    const idx = items.indexOf(current);
    if (idx < items.length - 1) this.selectedIndex.set(items[idx + 1]);
  }

  protected onArrowUp(event: Event): void {
    const ke = event as KeyboardEvent;
    if (!this.isOpen() || this.showConcedeConfirm()) return;
    ke.preventDefault();
    const items = this.navigableItems();
    const current = this.selectedIndex();
    const idx = items.indexOf(current);
    if (idx > 0) this.selectedIndex.set(items[idx - 1]);
  }

  protected onArrowRight(event: Event): void {
    const ke = event as KeyboardEvent;
    if (!this.isOpen() || this.showConcedeConfirm()) return;
    const item = this.menuItems[this.selectedIndex()];
    if (item?.type === 'slider') {
      ke.preventDefault();
      const current = this.sliderValue(item.id);
      this.onSliderChange(item.id, { target: { value: Math.min(100, current + 10) } } as unknown as Event);
    }
  }

  protected onArrowLeft(event: Event): void {
    const ke = event as KeyboardEvent;
    if (!this.isOpen() || this.showConcedeConfirm()) return;
    const item = this.menuItems[this.selectedIndex()];
    if (item?.type === 'slider') {
      ke.preventDefault();
      const current = this.sliderValue(item.id);
      this.onSliderChange(item.id, { target: { value: Math.max(0, current - 10) } } as unknown as Event);
    }
  }

  protected onEnter(event: Event): void {
    const ke = event as KeyboardEvent;
    if (!this.isOpen()) return;
    if (this.showConcedeConfirm()) return;
    ke.preventDefault();
    this.executeAction(this.selectedIndex());
  }

  protected onItemClick(index: number): void {
    this.selectedIndex.set(index);
    this.executeAction(index);
  }

  private navigableItems(): number[] {
    return this.menuItems
      .map((item, i) => ({ item, i }))
      .filter(({ item }) => item.type !== undefined)
      .map(({ i }) => i);
  }

  private executeAction(index: number): void {
    const item = this.menuItems[index];
    if (!item) return;
    switch (item.id) {
      case 'resume':
        this.closeMenu.emit();
        break;
      case 'music':
      case 'sfx':
        break;
      case 'concede':
        this.showConcedeConfirm.set(true);
        break;
    }
  }

  protected cancelConcede(): void {
    this.showConcedeConfirm.set(false);
  }

  protected confirmConcede(): void {
    this.showConcedeConfirm.set(false);
    this.concede.emit(this.matchId());
  }
}
