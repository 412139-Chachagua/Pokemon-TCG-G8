import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RankingEntry } from '../../../../shared/models/ranking.models';
import { RankingApiService } from '../../../../core/api/ranking-api.service';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-ranking-page',
  imports: [LoadingSpinnerComponent],
  template: `
    @if (loading()) {
      <app-loading-spinner />
    } @else {
      <table class="w-full border-collapse">
        <thead>
          <tr>
            <th class="ranking-title" colspan="7">RANKING</th>
          </tr>
          <tr>
            <th>#</th>
            <th>JUGADOR</th>
            <th>VICTORIAS</th>
            <th>DERROTAS</th>
            <th>WIN RATE</th>
            <th>RACHA</th>
            <th>MÁX RACHA</th>
          </tr>
        </thead>
        <tbody>
          @if (error(); as msg) {
            <tr>
              <td class="empty-row" colspan="7">{{ msg }}</td>
            </tr>
          } @else if (entries().length === 0) {
            <tr>
              <td class="empty-row" colspan="7">Ningún jugador tiene partidas registradas aún.</td>
            </tr>
          } @else {
            @for (entry of entries(); track entry.playerId) {
              <tr>
                <td class="rank">{{ entry.rank }}</td>
                <td class="player">{{ entry.displayName }}</td>
                <td class="wins">{{ entry.totalWins }}</td>
                <td class="losses">{{ entry.totalLosses }}</td>
                <td class="rate">{{ entry.winRate.toFixed(1) }}%</td>
                <td class="streak" [class.hot]="entry.currentWinStreak >= 3">
                  {{ entry.currentWinStreak > 0 ? entry.currentWinStreak + ' en racha' : '-' }}
                </td>
                <td class="max-streak">{{ entry.maxWinStreak > 0 ? entry.maxWinStreak : '-' }}</td>
              </tr>
            }
          }
        </tbody>
      </table>
    }
  `,
  styles: [`
    :host {
      display: block;
    }
    th, td {
      border: 1px solid rgba(255, 255, 255, 0.2);
      padding: 0.75rem 1rem;
      font-size: 0.7rem;
    }
    th {
      background: var(--pk-panel);
      color: var(--pk-text);
      text-align: center;
      font-weight: 600;
    }
    .ranking-title {
      font-size: 0.85rem;
      color: #ffffff;
      letter-spacing: 0.12em;
      padding: 0.6rem 1rem;
      border-bottom: 2px solid var(--pk-accent);
      text-shadow: 0 0 8px rgba(155, 109, 255, 0.3);
      background: rgba(0, 0, 0, 0.3);
    }
    td {
      text-align: center;
    }
    th:first-child, td:first-child {
      text-align: center;
    }
    th:nth-child(2), td:nth-child(2) {
      text-align: center;
    }
    tr:last-child td {
      border-bottom: none;
    }
    tr:hover td {
      background: var(--pk-surface);
    }
    .empty-row {
      text-align: center;
      color: var(--pk-text-dim);
      padding: 2rem 1rem;
      font-style: italic;
    }
    .rank {
      font-weight: 700;
      color: var(--pk-accent);
      width: 3rem;
    }
    .player {
      font-weight: 600;
      color: var(--pk-text-bright);
    }
    .wins {
      color: var(--pk-success);
      font-weight: 600;
    }
    .losses {
      color: var(--pk-error);
    }
    .rate {
      color: var(--pk-text-dim);
    }
    .streak {
      font-weight: 600;
    }
    .streak.hot {
      color: var(--pk-accent-glow);
    }
    .max-streak {
      color: var(--pk-text-dim);
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RankingPage {
  private readonly rankingApi = inject(RankingApiService);

  readonly entries = signal<RankingEntry[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() {
    this.loadRanking();
  }

  private loadRanking(): void {
    this.loading.set(true);
    this.error.set(null);
    this.rankingApi.getRanking().subscribe({
      next: (data) => {
        this.entries.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar el ranking');
        this.loading.set(false);
      },
    });
  }
}
