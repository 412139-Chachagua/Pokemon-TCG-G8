import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  template: `
    @if (totalPages() > 1) {
      <nav class="flex items-center gap-1">
        <button
          class="rounded-[var(--pk-radius)] border border-[var(--pk-btn-border)] bg-[var(--pk-btn)] px-[0.6rem] py-[0.35rem] font-[var(--pk-font)] text-[var(--pk-fz-sm)] text-[var(--pk-text)] hover:bg-[var(--pk-btn-hover)] disabled:cursor-not-allowed disabled:opacity-50"
          [disabled]="currentPage() === 0"
          (click)="goToPage(currentPage() - 1)"
        >
          &lt;
        </button>

        @for (page of visiblePages(); track page) {
          <button
            class="rounded-[var(--pk-radius)] border border-[var(--pk-btn-border)] px-[0.6rem] py-[0.35rem] font-[var(--pk-font)] text-[var(--pk-fz-sm)]"
            [class]="page === currentPage() ? 'bg-[var(--pk-accent)] text-white' : 'bg-[var(--pk-btn)] text-[var(--pk-text)] hover:bg-[var(--pk-btn-hover)]'"
            (click)="goToPage(page)"
          >
            {{ page + 1 }}
          </button>
        }

        <button
          class="rounded-[var(--pk-radius)] border border-[var(--pk-btn-border)] bg-[var(--pk-btn)] px-[0.6rem] py-[0.35rem] font-[var(--pk-font)] text-[var(--pk-fz-sm)] text-[var(--pk-text)] hover:bg-[var(--pk-btn-hover)] disabled:cursor-not-allowed disabled:opacity-50"
          [disabled]="currentPage() === totalPages() - 1"
          (click)="goToPage(currentPage() + 1)"
        >
          &gt;
        </button>
      </nav>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginationComponent {
  currentPage = input.required<number>();
  totalPages = input.required<number>();
  pageChange = output<number>();

  readonly visiblePages = computed(() => {
    const total = this.totalPages();
    const current = this.currentPage();
    const windowStart = Math.max(0, current - 2);
    const windowEnd = Math.min(total, windowStart + 5);
    return Array.from({ length: windowEnd - windowStart }, (_, i) => windowStart + i);
  });

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages() && page !== this.currentPage()) {
      this.pageChange.emit(page);
    }
  }
}
