import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { UserApiService } from '../api/user-api.service';
import { PlayerApiService } from '../api/player-api.service';
import { CreateUserRequest, UpdateUserRequest, UserResponse } from '../../shared/models/user.models';
import { PlayerResponse } from '../../shared/models/player.models';

const AUTH_STORAGE_KEY = 'auth_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userApi = inject(UserApiService);
  private readonly playerApi = inject(PlayerApiService);

  private readonly _user = signal<UserResponse | null>(null);
  private readonly _player = signal<PlayerResponse | null>(null);
  private readonly _isAuthenticated = signal(false);
  private readonly _token = signal<string | null>(null);

  readonly user = this._user.asReadonly();
  readonly player = this._player.asReadonly();
  readonly playerId = computed(() => this._player()?.id ?? null);
  readonly isAuthenticated = this._isAuthenticated.asReadonly();
  readonly token = this._token.asReadonly();

  constructor() {
    this.loadState();
  }

  register(request: CreateUserRequest): Observable<UserResponse> {
    return this.userApi.register(request).pipe(
      tap((user) => {
        this._user.set(user);
        this._token.set(user.token ?? null);
        this._isAuthenticated.set(true);
        this.loadPlayer(user.playerId);
        this.saveState(user);
      }),
    );
  }

  login(email: string, password: string): Observable<UserResponse> {
    return this.userApi.login({ email, password }).pipe(
      tap((user) => {
        this._user.set(user);
        this._token.set(user.token ?? null);
        this._isAuthenticated.set(true);
        this.loadPlayer(user.playerId);
        this.saveState(user);
      }),
    );
  }

  updatePlayer(playerId: string, displayName: string): Observable<PlayerResponse> {
    return this.playerApi.update(playerId, { displayName }).pipe(
      tap((updatedPlayer) => {
        this._player.set(updatedPlayer);
      }),
    );
  }

  updateAvatar(playerId: string, file: File): Observable<PlayerResponse> {
    return this.playerApi.uploadAvatar(playerId, file).pipe(
      tap((updatedPlayer) => {
        this._player.set(updatedPlayer);
      }),
    );
  }

  updateUser(userId: string, request: UpdateUserRequest): Observable<UserResponse> {
    return this.userApi.updateUser(userId, request).pipe(
      tap((updatedUser) => {
        this._user.set(updatedUser);
        this.saveState(updatedUser);
      }),
    );
  }

  deactivateUser(userId: string): Observable<UserResponse> {
    return this.userApi.deactivateUser(userId).pipe(
      tap(() => this.logout()),
    );
  }

  activateUser(userId: string, password: string): Observable<UserResponse> {
    return this.userApi.activateUser(userId, password);
  }

  validatePassword(userId: string, password: string): Observable<void> {
    return this.userApi.validatePassword(userId, password);
  }

  logout(): void {
    this._user.set(null);
    this._player.set(null);
    this._token.set(null);
    this._isAuthenticated.set(false);
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }

  private loadPlayer(playerId: string): void {
    this.playerApi.getById(playerId).subscribe({
      next: (player) => this._player.set(player),
      error: () => console.error('Error loading player'),
    });
  }

  private saveState(user: UserResponse): void {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user));
  }

  private loadState(): void {
    const stored = localStorage.getItem(AUTH_STORAGE_KEY);
    if (stored) {
      try {
        const user = JSON.parse(stored) as UserResponse;
        this._user.set(user);
        this._token.set(user.token ?? null);
        this._isAuthenticated.set(true);
        if (user.playerId) {
          Promise.resolve().then(() => this.loadPlayer(user.playerId));
        }
      } catch {
        localStorage.removeItem(AUTH_STORAGE_KEY);
      }
    }
  }
}
