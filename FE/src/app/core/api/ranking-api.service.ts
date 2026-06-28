import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from './api-client.service';
import { RankingEntry, PlayerStats } from '../../shared/models/ranking.models';

@Injectable({ providedIn: 'root' })
export class RankingApiService {
  private readonly apiClient = inject(ApiClientService);

  getRanking(): Observable<RankingEntry[]> {
    return this.apiClient.get<RankingEntry[]>('/ranking');
  }

  getPlayerStats(playerId: string): Observable<PlayerStats> {
    return this.apiClient.get<PlayerStats>(`/players/${playerId}/stats`);
  }
}
