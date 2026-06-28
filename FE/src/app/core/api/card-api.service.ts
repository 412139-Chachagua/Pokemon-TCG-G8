import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from './api-client.service';
import { CardDetailResponse, CardSummaryResponse, PaginatedCardsResponse } from '../../shared/models/card.models';

export interface CardSearchRequest {
  query?: string;
  supertype?: string;
  setCode?: string;
  stage?: string;
  page?: number;
  size?: number;
}

export type CardSearchResponse = PaginatedCardsResponse;

export interface CardSyncResponse {
  success: boolean;
  message: string;
  newCards: number;
  updatedCards: number;
}

@Injectable({ providedIn: 'root' })
export class CardApiService {
  private readonly apiClient = inject(ApiClientService);

  searchCards(request: CardSearchRequest): Observable<CardSearchResponse> {
    const params = new URLSearchParams();
    if (request.query) params.set('query', request.query);
    if (request.supertype) params.set('supertype', request.supertype);
    if (request.setCode) params.set('setCode', request.setCode);
    if (request.stage) params.set('stage', request.stage);
    if (request.page !== undefined) params.set('page', request.page.toString());
    if (request.size !== undefined) params.set('size', request.size.toString());
    return this.apiClient.get<CardSearchResponse>(`/cards?${params.toString()}`);
  }

  getCardById(cardId: string): Observable<CardDetailResponse> {
    return this.apiClient.get<CardDetailResponse>(`/cards/${cardId}`);
  }

  syncCards(): Observable<CardSyncResponse> {
    return this.apiClient.post<CardSyncResponse>('/cards/sync', {});
  }
}
