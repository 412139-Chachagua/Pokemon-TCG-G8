import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { MatchApiService, MatchResponse } from '../../../../core/api/match-api.service';

@Component({
  selector: 'app-match-list',
  standalone: true,
  imports: [],
  template: `
    <div class="p-4 border border-[var(--pk-dark)] rounded-lg">
      <div class="flex items-center justify-between mb-4">
        <h3 class="m-0 mb-4 text-lg text-[var(--pk-text)]">PARTIDAS DISPONIBLES</h3>
        <button class="pk-btn" (click)="onRefresh()">Actualizar</button>
      </div>

      @if (matches().length === 0) {
        <p class="text-[var(--pk-text-dim)] text-center py-8 m-0">No hay partidas disponibles.</p>
      } @else {
        <div>
          @for (match of matches(); track match.id) {
            <div class="flex items-center gap-4 py-2 border-b border-[var(--pk-dark)] last:border-b-0">
              <div class="flex-1 min-w-0">
                <div class="text-[var(--pk-fz-base)] text-[var(--pk-text)]">{{ match.hostName }}</div>
              </div>
              
              @if (match.hostId === playerId()) {
                <button class="pk-btn pk-btn--sm pk-btn--danger" (click)="onCancel(match.id)">
                  Cancelar
                </button>
              }
              @if (match.hostId !== playerId()) {
                <button class="pk-btn pk-btn--sm" (click)="matchSelected.emit({ id: match.id, hostName: match.hostName })">
                  Usar este
                </button>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MatchListComponent implements OnInit {
  private readonly matchApi = inject(MatchApiService);

  readonly playerId = input<string>('');
  readonly matchSelected = output<{ id: string; hostName: string }>();
  readonly matches = signal<{ id: string; status: string; hostName: string; hostId: string; createdAt: string }[]>([]);

  ngOnInit(): void {
    this.loadMatches();
  }

  onRefresh(): void {
    this.loadMatches();
  }

  private formatCreatedAt(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'hace unos segundos';
    if (mins < 60) return `hace ${mins} min`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `hace ${hours}h`;
    return new Date(iso).toLocaleDateString();
  }

  private loadMatches(): void {
    this.matchApi.listMatches().subscribe({
      next: (response: MatchResponse[]) => {
        this.matches.set(
          response.map((m: MatchResponse) => ({
            id: m.id,
            status: m.status,
            hostName: m.players?.[0]?.displayName ?? 'Unknown',
            hostId: m.players?.[0]?.playerId ?? '',
            createdAt: this.formatCreatedAt(m.createdAt),
          }))
        );
      },
      error: () => {
        this.matches.set([]);
      },
    });
  }

  onCancel(matchId: string): void {
    const pid = this.playerId();
    if (!pid) return;
    this.matchApi.deleteMatch(matchId, pid).subscribe({
      next: () => this.loadMatches(),
      error: () => this.loadMatches(),
    });
  }
}
