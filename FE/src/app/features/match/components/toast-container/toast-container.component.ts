import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from '../../../../shared/services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  template: `
    <div class="fixed top-20 right-4 z-50 flex flex-col gap-2 pointer-events-none">
      @for (toast of toastService.toasts(); track toast.id) {
        <div
          class="px-4 py-2 rounded-lg shadow-lg text-white text-sm font-semibold animate-[toast-slide-in_0.3s_ease-out] pointer-events-auto"
          [class.bg-red-600]="toast.type === 'hostile'"
          [class.bg-amber-500]="toast.type === 'reward'"
          [class.bg-green-600]="toast.type === 'heal'"
          [class.bg-blue-500]="toast.type === 'energy'"
          [class.bg-gray-700]="toast.type === 'info'"
        >
          {{ toast.text }}
        </div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastContainerComponent {
  protected readonly toastService = inject(ToastService);
}
