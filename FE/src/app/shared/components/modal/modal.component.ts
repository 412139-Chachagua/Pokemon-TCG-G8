import { ChangeDetectionStrategy, Component, effect, input, output, HostListener } from '@angular/core';
import { ClickOutsideDirective } from '../../directives/click-outside.directive';

@Component({
  selector: 'app-modal',
  imports: [ClickOutsideDirective],
  template: `
    @if (open()) {
      <div
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
        (clickOutside)="closed.emit()"
      >
        <div class="mx-4 w-full max-w-lg rounded-lg bg-white shadow-xl" (click)="$event.stopPropagation()">
          <div class="flex items-center justify-between border-b p-4">
            <h3 class="text-lg font-semibold">{{ title() }}</h3>
            <button
              class="text-gray-400 hover:text-gray-600"
              (click)="closed.emit()"
            >
              ✕
            </button>
          </div>
          <div class="p-4">
            <ng-content></ng-content>
          </div>
        </div>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalComponent {
  title = input.required<string>();
  open = input<boolean>(false);
  closed = output<void>();

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.open()) {
      this.closed.emit();
    }
  }
}
