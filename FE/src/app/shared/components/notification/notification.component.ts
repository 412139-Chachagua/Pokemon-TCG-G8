import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-notification',
  template: `
    <div class="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      @for (notif of notificationService.notifications(); track notif.id) {
        <div
          class="flex items-center gap-2 rounded-lg px-3 py-2 shadow-lg transition-all text-xs"
          [class]="getClasses(notif.type)"
        >
          <span class="flex-1">{{ notif.message }}</span>
          <button
            class="ml-1 text-current opacity-70 hover:opacity-100 text-xs"
            (click)="notificationService.dismiss(notif.id)"
          >
            ✕
          </button>
        </div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationComponent {
  protected readonly notificationService = inject(NotificationService);

  getClasses(type: string): string {
    const base = 'min-w-[180px]';
    switch (type) {
      case 'success': return `${base} bg-green-500 text-white`;
      case 'error': return `${base} bg-red-500 text-white`;
      case 'warning': return `${base} bg-yellow-500 text-black`;
      default: return `${base} bg-blue-500 text-white`;
    }
  }
}
