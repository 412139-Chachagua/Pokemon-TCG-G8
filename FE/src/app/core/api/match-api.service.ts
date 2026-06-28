import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from './api-client.service';
import { MatchStateResponse } from '../../shared/models/game-state.models';
import { GameActionRequest, GameActionResponse } from '../../shared/models/game-action.models';
import { MatchHistoryEntry } from '../../shared/models/match-history.models';
import { ChatMessage } from '../../shared/models/chat.models';

export interface MatchPlayerResponse {
  playerId: string;
  side: string;
  displayName: string;
}

export interface MatchResponse {
  id: string;
  status: string;
  currentPhase: string | null;
  turnNumber: number;
  currentPlayerId: string | null;
  firstPlayerId: string | null;
  winnerPlayerId: string | null;
  finishReason: string | null;
  players: MatchPlayerResponse[];
  createdAt: string;
  lastSavedAt: string | null;
  lastResumedPlayerId: string | null;
}

export interface CreateMatchRequest {
  player1Id: string;
  player1Name: string;
  player1DeckId: string;
  quickMatch?: boolean;
  player2Name?: string;
  player2DeckId?: string;
}

export interface JoinMatchRequest {
  playerId: string;
  playerName: string;
  deckId: string;
}

@Injectable({ providedIn: 'root' })
export class MatchApiService {
  private readonly apiClient = inject(ApiClientService);

  createMatch(request: CreateMatchRequest): Observable<MatchResponse> {
    return this.apiClient.post<MatchResponse>('/matches', request);
  }

  joinMatch(matchId: string, request: JoinMatchRequest): Observable<MatchResponse> {
    return this.apiClient.post<MatchResponse>(`/matches/${matchId}/join`, request);
  }

  getMatchState(matchId: string, playerId: string): Observable<MatchStateResponse> {
    return this.apiClient.get<MatchStateResponse>(`/matches/${matchId}/state?playerId=${playerId}`);
  }

  getActiveMatches(playerId: string): Observable<MatchResponse[]> {
    return this.apiClient.get<MatchResponse[]>(`/matches/active?playerId=${playerId}`);
  }

  listMatches(status?: string): Observable<MatchResponse[]> {
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    const query = params.toString();
    return this.apiClient.get<MatchResponse[]>(`/matches${query ? '?' + query : ''}`);
  }

  deleteMatch(matchId: string, playerId: string): Observable<MatchResponse> {
    return this.apiClient.delete<MatchResponse>(`/matches/${matchId}?playerId=${playerId}`);
  }

  concedeMatch(matchId: string, playerId: string): Observable<MatchResponse> {
    return this.apiClient.post<MatchResponse>(`/matches/${matchId}/concede?playerId=${playerId}`, {});
  }

  sendAction(matchId: string, action: GameActionRequest): Observable<GameActionResponse> {
    return this.apiClient.post<GameActionResponse>(`/matches/${matchId}/actions`, action);
  }

  getHistory(playerId: string): Observable<MatchHistoryEntry[]> {
    return this.apiClient.get<MatchHistoryEntry[]>(`/matches/history?playerId=${playerId}`);
  }

  getChatHistory(matchId: string): Observable<ChatMessage[]> {
    return this.apiClient.get<ChatMessage[]>(`/matches/${matchId}/chat`);
  }
}
