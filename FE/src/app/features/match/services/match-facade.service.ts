import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { MatchApiService, MatchResponse } from '../../../core/api/match-api.service';
import { AuthService } from '../../../core/services/auth.service';
import { MatchStateService } from './match-state.service';

@Injectable({ providedIn: 'root' })
export class MatchFacadeService {
  private readonly matchApi = inject(MatchApiService);
  private readonly authService = inject(AuthService);
  private readonly matchState = inject(MatchStateService);

  private readonly _matchId = signal<string | null>(null);
  readonly matchId = this._matchId.asReadonly();

  private readonly _playerId = signal<string | null>(null);
  readonly playerId = this._playerId.asReadonly();

  private readonly _side = signal<string | null>(null);
  readonly side = this._side.asReadonly();

  private readonly _status = signal<string | null>(null);
  readonly status = this._status.asReadonly();

  private readonly _playerName = signal<string | null>(null);
  readonly playerName = this._playerName.asReadonly();

  private readonly _deckName = signal<string | null>(null);
  readonly deckName = this._deckName.asReadonly();

  createMatch(player1Name: string, player1DeckId: string, deckName: string): Observable<MatchResponse> {
    const player1Id = this.authService.playerId();
    this._playerName.set(player1Name);
    this._deckName.set(deckName);
    return this.matchApi.createMatch({ player1Id: player1Id ?? '', player1Name, player1DeckId }).pipe(
      tap((res) => {
        const player = res.players[0];
        this._matchId.set(res.id);
        this._playerId.set(player?.playerId ?? null);
        this._side.set(player?.side ?? null);
        this._status.set(res.status);
      }),
    );
  }

  createQuickMatch(deckId: string): Observable<MatchResponse> {
    const playerId = this.authService.playerId();
    const playerName = this.authService.player()?.displayName ?? '';
    return this.matchApi.createMatch({
      player1Id: playerId ?? '',
      player1Name: playerName,
      player1DeckId: deckId,
      quickMatch: true,
    }).pipe(tap((res) => {
      const player = res.players[0];
      this._matchId.set(res.id);
      this._playerId.set(player?.playerId ?? null);
      this._side.set(player?.side ?? null);
      this._status.set(res.status);
    }));
  }

  joinMatch(matchId: string, playerName: string, deckId: string): Observable<MatchResponse> {
    const playerId = this.authService.playerId();
    return this.matchApi.joinMatch(matchId, { playerId: playerId ?? '', playerName, deckId }).pipe(
      tap((res) => {
        const player = res.players.find(p => p.playerId === this._playerId()) ?? res.players[0];
        this._matchId.set(res.id);
        this._playerId.set(player?.playerId ?? null);
        this._side.set(player?.side ?? null);
        this._status.set(res.status);
      }),
    );
  }

  getMatchState(): void {
    const mId = this._matchId();
    if (!mId) {
      return;
    }
    this.matchState.initialize(mId);
  }

  reset(): void {
    this._matchId.set(null);
    this._playerId.set(null);
    this._side.set(null);
    this._status.set(null);
  }
}
