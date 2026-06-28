import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-victory-overlay',
  template: `
    <div class="backdrop">
      <div class="overlay-card">
        <h2 class="title">Fin de partida</h2>
        <p class="result" [class.win]="isWinner()" [class.lose]="!isWinner()">
          @if (isWinner()) {
            ¡Has ganado! {{ opponentName() }} ha sido derrotado.
          } @else {
            ¡Has perdido! {{ opponentName() }} ha ganado la partida.
          }
        </p>
        <button class="btn-lobby" [disabled]="lobbyClicked" (click)="onLobbyClick()">
          Volver al lobby
        </button>
      </div>
    </div>
  `,
  styles: [`
    .backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.7);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }
    .overlay-card {
      background: #1e293b;
      border: 2px solid #475569;
      border-radius: 0.75rem;
      padding: 2.5rem;
      text-align: center;
      max-width: 400px;
      width: 90%;
    }
    .title {
      margin: 0 0 1rem;
      font-size: 1.5rem;
      color: #e2e8f0;
    }
    .result {
      font-size: 1.25rem;
      font-weight: 700;
      margin: 0 0 2rem;
    }
    .result.win {
      color: #22c55e;
    }
    .result.lose {
      color: #ef4444;
    }
    .btn-lobby {
      padding: 0.75rem 2rem;
      border: none;
      border-radius: 0.5rem;
      background: #2563eb;
      color: #fff;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.15s;
      font-family: inherit;
    }
    .btn-lobby:hover {
      background: #3b82f6;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VictoryOverlayComponent {
  readonly winnerPlayerId = input<string | null>(null);
  readonly myPlayerId = input<string | null>(null);
  readonly opponentName = input<string>('El oponente');

  readonly returnToLobby = output<void>();

  protected lobbyClicked = false;

  protected readonly isWinner = computed(() => {
    return this.winnerPlayerId() !== null && this.winnerPlayerId() === this.myPlayerId();
  });

  protected onLobbyClick(): void {
    if (this.lobbyClicked) return;
    this.lobbyClicked = true;
    this.returnToLobby.emit();
  }
}
