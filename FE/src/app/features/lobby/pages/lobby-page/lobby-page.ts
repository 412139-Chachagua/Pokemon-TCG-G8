import { ChangeDetectionStrategy, Component, inject, signal, OnDestroy, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { BackButtonComponent } from '../../../../shared/components/back-button/back-button.component';
import { AuthService } from '../../../../core/services/auth.service';
import { MatchStateService } from '../../../match/services/match-state.service';
import { MatchFacadeService } from '../../../match/services/match-facade.service';
import { MatchApiService, MatchResponse } from '../../../../core/api/match-api.service';
import { MatchCreateComponent } from '../../components/match-create/match-create.component';
import { MatchJoinComponent } from '../../components/match-join/match-join.component';
import { MatchListComponent } from '../../components/match-list/match-list.component';

@Component({
  selector: 'app-lobby-page',
  standalone: true,
  imports: [MatchCreateComponent, MatchJoinComponent, MatchListComponent, RouterLink, BackButtonComponent],
  template: `
    <div class="relative min-h-dvh">
      <app-back-button />
      <main class="min-h-dvh bg-[var(--pk-bg)] p-6 max-w-4xl mx-auto">
        @if (!authService.isAuthenticated()) {
          <div class="flex flex-col items-center gap-4 text-center px-4 py-12">
            <p class="pk-text-dim">Iniciá sesión para jugar</p>
            <a routerLink="/auth/register" class="pk-btn">Ir a registro</a>
          </div>
        } @else {
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
            <div class="pk-panel">
              <div class="pk-panel__header">CREAR PARTIDA</div>
              <div class="pk-panel__body">
                <app-match-create
                  [playerId]="authService.playerId() ?? ''"
                  [preSelectedDeckId]="preSelectedDeckId"
                  (created)="onMatchCreated($event)"
                />
              </div>
            </div>
            <div class="pk-panel">
              <div class="pk-panel__header">PARTIDAS DISPONIBLES</div>
              <div class="pk-panel__body">
                <app-match-list [playerId]="authService.playerId() ?? ''" (matchSelected)="onMatchSelected($event)" />
              </div>
            </div>
          </div>
          <div class="grid gap-4 mb-4">
            <div class="pk-panel">
              <div class="pk-panel__header">UNIRSE A PARTIDA</div>
              <div class="pk-panel__body">
                <app-match-join
                  [playerId]="authService.playerId() ?? ''"
                  (joined)="onMatchJoined($event)"
                />
              </div>
            </div>
          </div>
          <div class="pk-panel">
            <div class="pk-panel__header">PARTIDAS EN CURSO</div>
            <div class="pk-panel__body">
            <div class="p-4 border border-[var(--pk-dark)] rounded-lg">
              <h3 class="m-0 mb-4 text-lg text-[var(--pk-text)]">PARTIDAS EN CURSO</h3>
              @if (activeMatches().length === 0) {
                <p class="text-[var(--pk-text-dim)] text-sm text-center py-4">No hay partidas en curso</p>
              } @else {
                <div class="max-h-[220px] overflow-y-auto">
                  @for (match of activeMatches(); track match.id) {
                  <div class="flex items-center gap-2 py-2 border-b border-[var(--pk-dark)] last:border-b-0">
                    <div class="flex-1 min-w-0">
                      <div class="text-sm text-[var(--pk-text)]">
                        vs {{ opponentName(match) }}
                      </div>
                      <div class="text-sm text-[var(--pk-text-dim)]">
                        Turno {{ match.turnNumber }} · {{ match.currentPhase }}
                        @if (match.lastSavedAt) {
                          · {{ formatLastSaved(match.lastSavedAt) }}
                        }
                      </div>
                    </div>
                      @if (opponentResumed(match)) {
                        <button class="pk-btn pk-btn--sm !bg-green-600 !border-green-500 hover:!bg-green-500" (click)="resumeMatch(match.id)">
                          En juego
                        </button>
                      } @else if (iResumed(match)) {
                        <button class="pk-btn pk-btn--sm opacity-50 cursor-not-allowed" disabled title="Esperando al oponente...">
                          Esperando...
                        </button>
                      } @else {
                        <button class="pk-btn pk-btn--sm" (click)="resumeMatch(match.id)">
                          Reanudar
                        </button>
                      }
                      @if (isCreator(match)) {
                        <button class="flex h-7 w-7 items-center justify-center rounded-md bg-red-700/80 text-white text-sm hover:bg-red-600 cursor-pointer border-none leading-none" (click)="deleteMatch(match.id)" title="Eliminar partida">
                          ✕
                        </button>
                      } @else {
                        <button type="button" disabled class="flex h-7 w-7 items-center justify-center rounded-md bg-gray-700/30 text-gray-500 text-sm cursor-not-allowed border-none leading-none" title="Solo {{ creatorName(match) }} puede eliminar la partida" (click)="showDeleteInfo(match)">
                          ✕
                        </button>
                      }
                    </div>
                  }
                </div>
              }
            </div>
            </div>
          </div>
        }
      </main>
    </div>
  `,
  styles: [`
    :host {
      animation: pk-fade-in 0.35s ease-out both;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LobbyPage {
  private readonly matchState = inject(MatchStateService);
  private readonly matchFacade = inject(MatchFacadeService);
  private readonly matchApi = inject(MatchApiService);
  private readonly router = inject(Router);

  protected readonly authService = inject(AuthService);
  protected readonly activeMatches = signal<MatchResponse[]>([]);
  protected preSelectedDeckId: string | null = null;
  private readonly matchJoin = viewChild(MatchJoinComponent);

  private pollTimer: ReturnType<typeof setInterval> | null = null;

  constructor() {
    const params = new URLSearchParams(window.location.search);
    this.preSelectedDeckId = params.get('deckId');
    this.loadActiveMatches();
    this.pollTimer = setInterval(() => this.loadActiveMatches(), 3000);
  }

  onMatchCreated(response: { id: string }): void {
    this.matchState.reset();
    this.matchFacade.reset();
    this.router.navigate(['/match', response.id], { replaceUrl: true });
  }

  onMatchJoined(response: { id: string }): void {
    this.matchState.reset();
    this.matchFacade.reset();
    this.router.navigate(['/match', response.id], { replaceUrl: true });
  }

  onMatchSelected(match: { id: string; hostName: string }): void {
    this.matchJoin()?.selectMatch(match.id, match.hostName);
  }

  private loadActiveMatches(): void {
    const pid = this.authService.playerId();
    if (!pid) return;
    this.matchApi.getActiveMatches(pid).subscribe({
      next: (matches) => this.activeMatches.set(matches),
      error: () => this.activeMatches.set([]),
    });
  }

  protected isCreator(match: MatchResponse): boolean {
    return match.players?.[0]?.playerId === this.authService.playerId();
  }

  protected creatorName(match: MatchResponse): string {
    return match.players?.[0]?.displayName ?? 'Creador';
  }

  protected opponentName(match: MatchResponse): string {
    return match.players?.[1]?.displayName ?? match.players?.[0]?.displayName ?? 'Oponente';
  }

  protected opponentResumed(match: MatchResponse): boolean {
    const pid = this.authService.playerId();
    if (!pid || !match.lastResumedPlayerId) return false;
    return match.lastResumedPlayerId !== pid;
  }

  protected iResumed(match: MatchResponse): boolean {
    const pid = this.authService.playerId();
    if (!pid || !match.lastResumedPlayerId) return false;
    return match.lastResumedPlayerId === pid;
  }

  protected formatLastSaved(iso: string | null): string {
    if (!iso) return '';
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'hace segundos';
    if (mins < 60) return `hace ${mins} min`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `hace ${hours}h`;
    return new Date(iso).toLocaleDateString();
  }

  resumeMatch(matchId: string): void {
    this.matchState.reset();
    this.matchFacade.reset();
    this.router.navigate(['/match', matchId], { queryParams: { rejoin: 'true' }, replaceUrl: true });
  }

  deleteMatch(matchId: string): void {
    if (!window.confirm('¿Estás seguro de eliminar esta partida?')) return;
    const pid = this.authService.playerId();
    if (!pid) return;
    this.matchApi.deleteMatch(matchId, pid).subscribe({
      next: () => this.loadActiveMatches(),
      error: () => this.loadActiveMatches(),
    });
  }

  showDeleteInfo(match: MatchResponse): void {
    alert(`Solo ${this.creatorName(match)} puede eliminar esta partida`);
  }

  ngOnDestroy(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  protected goToHub(): void {
    this.router.navigate(['/home']);
  }
}
