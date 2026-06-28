import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatchHistoryEntry } from '../../../../shared/models/match-history.models';
import { AuthService } from '../../../../core/services/auth.service';
import { MatchApiService } from '../../../../core/api/match-api.service';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-history-page',
  imports: [LoadingSpinnerComponent],
  template: `
    @if (loading()) {
      <app-loading-spinner />
    } @else {
      <table class="w-full border-collapse">
        <thead>
          <tr>
            <th class="ranking-title" colspan="7">HISTORIAL</th>
          </tr>
          <tr>
            <th>#</th>
            <th>FECHA</th>
            <th>DURACIÓN</th>
            <th>RIVAL</th>
            <th>RESULTADO</th>
            <th>MOTIVO</th>
            <th>TURNOS</th>
          </tr>
        </thead>
        <tbody>
          @if (error(); as msg) {
            <tr>
              <td class="empty-row" colspan="7">{{ msg }}</td>
            </tr>
          } @else if (entries().length === 0) {
            <tr>
              <td class="empty-row" colspan="7">No tenés partidas registradas aún.</td>
            </tr>
          } @else {
            @for (entry of entries(); track entry.id; let i = $index) {
              <tr>
                <td class="rank">{{ i + 1 }}</td>
                <td class="date">{{ formatDate(entry.createdAt) }}</td>
                <td class="duration">{{ formatDuration(entry.durationSeconds) }}</td>
                <td class="opponent">{{ opponentName(entry) }}</td>
                <td class="result" [class.win]="isWin(entry)" [class.loss]="isLoss(entry)" [class.draw]="isDraw(entry)">
                  {{ resultLabel(entry) }}
                </td>
                <td class="reason">{{ reasonLabel(entry.finishReason) }}</td>
                <td class="turns">{{ entry.totalTurns }}</td>
              </tr>
            }
          }
        </tbody>
      </table>
    }
  `,
  styles: [`
    :host { display: block; padding: 3px 0.25rem 1rem 0.25rem; }
    th, td {
      border: 1px solid rgba(255, 255, 255, 0.2);
      padding: 0.95rem 1rem;
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
    td { text-align: center; }
    th:first-child, td:first-child { text-align: center; }
    th:nth-child(4), td:nth-child(4) { text-align: center; }
    tr:last-child td { border-bottom: none; }
    tr:hover td { background: var(--pk-surface); }
    .empty-row {
      text-align: center;
      color: var(--pk-text-dim);
      padding: 2rem 1rem;
      font-style: italic;
    }
    .rank { font-weight: 700; color: var(--pk-accent); width: 3rem; }
    .opponent { font-weight: 600; color: var(--pk-text-bright); }
    .result { font-weight: 600; }
    .result.win { color: var(--pk-success); }
    .result.loss { color: var(--pk-error); }
    .result.draw { color: var(--pk-text-dim); }
    .date, .duration, .reason, .turns { color: var(--pk-text-dim); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HistoryPage {
  private readonly auth = inject(AuthService);
  private readonly matchApi = inject(MatchApiService);
  private readonly router = inject(Router);

  readonly entries = signal<MatchHistoryEntry[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly myPlayerId = this.auth.playerId;

  constructor() {
    this.loadHistory();
  }

  goHome(): void {
    this.router.navigate(['/home']);
  }

  private loadHistory(): void {
    const pid = this.myPlayerId();
    if (!pid) { this.error.set('No se pudo identificar al jugador'); this.loading.set(false); return; }

    this.loading.set(true);
    this.error.set(null);
    this.matchApi.getHistory(pid).subscribe({
      next: (data) => { this.entries.set(data); this.loading.set(false); },
      error: () => { this.error.set('Error al cargar el historial'); this.loading.set(false); },
    });
  }

  isDraw(entry: MatchHistoryEntry): boolean {
    return ['CANCELLED', 'EXPIRED', 'SUDDEN_DEATH'].includes(entry.finishReason ?? '');
  }

  isWin(entry: MatchHistoryEntry): boolean {
    return !this.isDraw(entry) && entry.winnerName === this.auth.player()?.displayName;
  }

  isLoss(entry: MatchHistoryEntry): boolean {
    return !this.isDraw(entry) && entry.loserName === this.auth.player()?.displayName;
  }

  opponentName(entry: MatchHistoryEntry): string {
    const myName = this.auth.player()?.displayName ?? '';
    return entry.winnerName === myName ? entry.loserName : entry.winnerName;
  }

  resultLabel(entry: MatchHistoryEntry): string {
    if (this.isDraw(entry)) return 'Empate';
    if (this.isWin(entry)) return 'Victoria';
    return 'Derrota';
  }

  formatDuration(seconds: number | null): string {
    if (seconds == null) return '-';
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return m + 'm ' + s + 's';
  }

  formatDate(iso: string): string {
    if (!iso) return '-';
    const d = new Date(iso);
    const pad = (n: number) => n.toString().padStart(2, '0');
    return pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + '/' + pad(d.getFullYear() % 100)
      + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
  }

  reasonLabel(reason: string | null): string {
    switch (reason) {
      case 'KNOCKOUT': return 'KO';
      case 'PRIZES': return 'Premios';
      case 'DECK_OUT': return 'Sin cartas';
      case 'CONCEDE': return 'Rendición';
      case 'EXPIRED': return 'Tiempo agotado';
      case 'CANCELLED': return 'Cancelada';
      default: return '-';
    }
  }
}
