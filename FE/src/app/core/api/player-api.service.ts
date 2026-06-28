import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from './api-client.service';
import { PlayerResponse, UpdatePlayerRequest } from '../../shared/models/player.models';

@Injectable({ providedIn: 'root' })
export class PlayerApiService {
  private readonly apiClient = inject(ApiClientService);
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api';

  listAll(): Observable<PlayerResponse[]> {
    return this.apiClient.get<PlayerResponse[]>('/players');
  }

  getById(id: string): Observable<PlayerResponse> {
    return this.apiClient.get<PlayerResponse>(`/players/${id}`);
  }

  update(id: string, request: UpdatePlayerRequest): Observable<PlayerResponse> {
    return this.apiClient.put<PlayerResponse>(`/players/${id}`, request);
  }

  uploadAvatar(playerId: string, file: File): Observable<PlayerResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<PlayerResponse>(
      `${this.baseUrl}/players/${playerId}/avatar`,
      formData,
    );
  }
}
