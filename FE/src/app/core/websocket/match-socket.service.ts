import { inject, Injectable } from '@angular/core';
import { Client, IFrame, IMessage, StompSubscription } from '@stomp/stompjs';
import { Observable, Subject } from 'rxjs';
import SockJS from 'sockjs-client';
import { ChatMessage } from '../../shared/models/chat.models';
import { GameActionResponse, GameEventDto } from '../../shared/models/game-action.models';
import { PrivatePlayerStateModel } from '../../shared/models/game-state.models';
import { AuthService } from '../services/auth.service';

export type ConnectionStatus = 'CONNECTED' | 'DISCONNECTED' | 'RECONNECTING';

@Injectable({ providedIn: 'root' })
export class MatchSocketService {
  private readonly brokerUrl = 'http://localhost:8080/ws';
  private readonly auth = inject(AuthService);

  private client: Client | null = null;
  private publicSub: StompSubscription | null = null;
  private privateSub: StompSubscription | null = null;
  private chatSub: StompSubscription | null = null;

  private readonly _publicEvents = new Subject<GameEventDto>();
  readonly publicEvents$ = this._publicEvents.asObservable();

  private readonly _privateState = new Subject<PrivatePlayerStateModel>();
  readonly privateState$ = this._privateState.asObservable();

  private readonly _actionErrors = new Subject<GameActionResponse['error']>();
  readonly actionErrors$ = this._actionErrors.asObservable();

  private readonly _chatMessages = new Subject<ChatMessage>();
  readonly chatMessages$ = this._chatMessages.asObservable();

  private readonly _connectionStatus = new Subject<ConnectionStatus>();
  readonly connectionStatus$ = this._connectionStatus.asObservable();

  private currentMatchId: string | null = null;
  private currentPlayerId: string | null = null;

  connect(matchId: string, playerId: string): void {
    if (this.client?.active) {
      this.disconnect();
    }

    this.currentMatchId = matchId;
    this.currentPlayerId = playerId;
    this._connectionStatus.next('RECONNECTING');

    const token = this.auth.token();
    const connectHeaders: Record<string, string> = {};
    if (token) {
      connectHeaders['Authorization'] = `Bearer ${token}`;
    }

    this.client = new Client({
      webSocketFactory: () => new SockJS(this.brokerUrl),
      connectHeaders,
      reconnectDelay: 2000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => this.onConnected(),
      onDisconnect: () => this._connectionStatus.next('DISCONNECTED'),
      onStompError: (frame: IFrame) => {
        console.error('STOMP error:', frame.headers['message']);
        this._connectionStatus.next('DISCONNECTED');
      },
    });

    this.client.activate();
  }

  disconnect(): void {
    this.unsubscribeAll();
    this.client?.deactivate();
    this.client = null;
    this.currentMatchId = null;
    this.currentPlayerId = null;
    this._connectionStatus.next('DISCONNECTED');
  }

  sendChatMessage(message: ChatMessage): boolean {
    if (!this.client?.active || !this.currentMatchId) {
      return false;
    }
    this.client.publish({
      destination: `/app/matches/${this.currentMatchId}/chat`,
      body: JSON.stringify(message),
    });
    return true;
  }

  sendAction(action: unknown): boolean {
    if (!this.client?.active || !this.currentMatchId) {
      console.error('[MatchSocket] Cannot send action: WebSocket not active', {
        clientActive: this.client?.active,
        currentMatchId: this.currentMatchId,
      });
      return false;
    }
    this.client.publish({
      destination: `/app/matches/${this.currentMatchId}/actions`,
      body: JSON.stringify(action),
    });
    return true;
  }

  private onConnected(): void {
    this._connectionStatus.next('CONNECTED');

    if (this.currentMatchId && this.currentPlayerId) {
      this.subscribeToMatch(this.currentMatchId, this.currentPlayerId);
    }
  }

  private subscribeToMatch(matchId: string, playerId: string): void {
    if (!this.client) {
      return;
    }

    this.publicSub = this.client.subscribe(
      `/topic/matches/${matchId}/events`,
      (message: IMessage) => {
        try {
          const response = JSON.parse(message.body);
          console.log('[DEBUG] WebSocket /events received:', JSON.stringify({
            hasPublicState: !!response.publicState,
            hasError: !!response.error,
            hasPrivateState: !!response.privateState,
            eventCount: response.events?.length,
            success: response.success,
          }));

          // Standalone event (e.g., MULLIGAN_REVEALED published directly
          // by EventPublisherPort during SetupManager.setup())
          if (response.type && !response.publicState && !Array.isArray(response.events)) {
            this._publicEvents.next({
              type: response.type,
              message: response.message ?? response.type,
              payload: response.payload,
            });
            return;
          }

          if (response.publicState) {
            this._publicEvents.next({
              type: 'STATE_UPDATED',
              message: 'State updated',
              payload: { publicState: response.publicState },
            });
          }
          if (response.error) {
            this._actionErrors.next(response.error);
          }
          if (Array.isArray(response.events)) {
            for (const event of response.events) {
              if (typeof event === 'string') {
                if (event !== 'STATE_UPDATED') {
                  this._publicEvents.next({ type: event, message: event });
                }
              } else if (event.type !== 'STATE_UPDATED') {
                this._publicEvents.next(event as GameEventDto);
              }
            }
          }
          if (response.privateState) {
            this._privateState.next(response.privateState);
          }
        } catch (e) {
          console.error('[DEBUG] WebSocket /events parse error:', e);
        }
      },
    );

    this.privateSub = this.client.subscribe(
      `/topic/matches/${matchId}/player/${playerId}`,
      (message: IMessage) => {
        try {
          const state: PrivatePlayerStateModel = JSON.parse(message.body);
          console.log('[DEBUG] WebSocket privateState received, hand size:', state.hand?.length);
          this._privateState.next(state);
        } catch (e) {
          console.error('[DEBUG] WebSocket privateState parse error:', e);
        }
      },
    );

    this.chatSub = this.client.subscribe(
      `/topic/matches/${matchId}/chat`,
      (message: IMessage) => {
        try {
          const chatMsg: ChatMessage = JSON.parse(message.body);
          this._chatMessages.next(chatMsg);
        } catch {
          // ignore parse errors
        }
      },
    );
  }

  private unsubscribeAll(): void {
    this.publicSub?.unsubscribe();
    this.publicSub = null;
    this.privateSub?.unsubscribe();
    this.privateSub = null;
    this.chatSub?.unsubscribe();
    this.chatSub = null;
  }
}
