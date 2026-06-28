import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  template: `
    <div class="flex flex-col items-center justify-center p-8">
      <div class="pokeball"></div>
      <p class="mt-4 text-[var(--pk-text)]">{{ text() }}</p>
    </div>
  `,
  styles: [`
    .pokeball {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      background: linear-gradient(to bottom, #ee1515 0%, #ee1515 50%, #f0f0f0 50%, #f0f0f0 100%);
      border: 3px solid #222;
      position: relative;
      animation: spin 1s linear infinite;
    }
    .pokeball::before {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 18px;
      height: 18px;
      background: #f0f0f0;
      border-radius: 50%;
      border: 3px solid #222;
    }
    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoadingSpinnerComponent {
  readonly text = input('Cargando...');
}
