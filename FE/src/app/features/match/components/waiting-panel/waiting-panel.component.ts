import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-waiting-panel',
  standalone: true,
  template: `
    <div class="pk-waiting-panel">
      <p class="title">Buscando jugador...</p>
      <div class="pokeball"></div>
      <div class="player-info">
        <span class="player-name">{{ playerName() }}</span>
        <span class="deck-name">Mazo: {{ deckName() }}</span>
      </div>
      <button class="pk-btn pk-btn--danger" (click)="cancelMatch.emit()">Cancelar partida</button>
    </div>
  `,
  styles: [`
    :host {
      display: contents;
    }
    .pk-waiting-panel {
      background: linear-gradient(135deg, #2d1b4e 0%, #1a0a2e 100%);
      border: 2px solid #FFCB05;
      border-radius: 16px;
      padding: 2.5rem 3rem;
      display: flex;
      flex-direction: column;
      align-items: center;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
      max-width: 600px;
      width: 100%;
    }
    .title {
      font-size: 1.25rem;
      font-weight: 700;
      color: #FFCB05;
      margin: 0 0 1.5rem 0;
      white-space: nowrap;
    }
    .pokeball {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      background: linear-gradient(to bottom, #ee1515 0%, #ee1515 50%, #f0f0f0 50%, #f0f0f0 100%);
      border: 3px solid #222;
      position: relative;
      animation: spin 1s linear infinite;
      flex-shrink: 0;
      margin-bottom: 1.5rem;
    }
    .pokeball::before {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 16px;
      height: 16px;
      background: #f0f0f0;
      border-radius: 50%;
      border: 3px solid #222;
    }
    .player-info {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 0.25rem;
      margin-bottom: 2rem;
      width: 100%;
    }
    .player-name {
      font-size: 1rem;
      color: var(--pk-text, #ffffff);
    }
    .deck-name {
      font-size: 0.875rem;
      color: var(--pk-text-dim, #b8a0d0);
    }
    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WaitingPanelComponent {
  readonly playerName = input<string>('Jugador');
  readonly deckName = input<string>('—');
  readonly cancelMatch = output<void>();
}
