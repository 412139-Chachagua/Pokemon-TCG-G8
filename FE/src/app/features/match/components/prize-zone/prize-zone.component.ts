import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-prize-zone',
  standalone: true,
  template: `
    <div class="grid justify-items-center h-full grid-cols-1" [style.grid-template-rows]="'repeat(' + totalPrizeCount() + ', 1fr)'">
      @for (slot of slots; track $index) {
        @if (slot) {
          <img [src]="cardBackUrl" alt="Premio" class="h-full w-auto max-w-full object-contain" />
        } @else {
          <div></div>
        }
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrizeZoneComponent {
  readonly prizeCount = input.required<number>();
  readonly isOwn = input.required<boolean>();
  readonly totalPrizeCount = input<number>(6);
  readonly columns = input<1 | 2 | 3>(2);

  protected readonly cardBackUrl = 'assets/images/card-back.svg';

  get slots(): boolean[] {
    const total = this.totalPrizeCount();
    const remaining = this.prizeCount();
    return Array.from({ length: total }, (_, i) => i < remaining);
  }
}