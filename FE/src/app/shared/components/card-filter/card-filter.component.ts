import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

export interface FilterOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-card-filter',
  template: `
      <select
        #filterSelect
        [disabled]="disabled()"
        class="w-full rounded-md border border-[var(--pk-btn-border)] bg-[var(--pk-surface)] px-4 py-2 text-[length:var(--pk-fz-base)] text-[var(--pk-text)] focus:border-[var(--pk-accent)] focus:outline-none focus:ring-1 focus:ring-[var(--pk-accent)] disabled:opacity-40 disabled:cursor-not-allowed"
        (change)="onChange(filterSelect.value)"
      >
        @for (option of options(); track option.value) {
          <option [value]="option.value">
            {{ option.label }}
          </option>
        }
      </select>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardFilterComponent {
  options = input<FilterOption[]>([]);
  selected = input<string>('');
  disabled = input(false);
  filterChange = output<string>();

  onChange(value: string): void {
    this.filterChange.emit(value);
  }
}
