import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-search-bar',
  template: `
    <div class="relative">
      <input
        type="text"
        [placeholder]="placeholder()"
        class="w-full rounded-md border border-[var(--pk-btn-border)] bg-[var(--pk-surface)] px-4 py-2 pr-8 text-[length:var(--pk-fz-base)] text-[var(--pk-text)] placeholder-[var(--pk-text-dim)] focus:border-[var(--pk-accent)] focus:outline-none focus:ring-1 focus:ring-[var(--pk-accent)]"
        (input)="onInput($event)"
      />
      @if (value()) {
        <button
          class="absolute right-2 top-1/2 -translate-y-1/2 text-[var(--pk-text-dim)] hover:text-[var(--pk-text)]"
          (click)="clear()"
        >
          ✕
        </button>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchBarComponent {
  placeholder = input('Buscar cartas...');
  queryChange = output<string>();

  readonly value = signal('');

  private readonly input$ = new Subject<string>();
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.input$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(value => this.queryChange.emit(value));
  }

  onInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.value.set(value);
    this.input$.next(value);
  }

  clear(): void {
    this.value.set('');
    this.input$.next('');
  }
}
