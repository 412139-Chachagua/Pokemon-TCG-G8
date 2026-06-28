import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from './api-client.service';
import { CreateUserRequest, LoginRequest, UpdateUserRequest, UserResponse } from '../../shared/models/user.models';

@Injectable({ providedIn: 'root' })
export class UserApiService {
  private readonly apiClient = inject(ApiClientService);

  register(request: CreateUserRequest): Observable<UserResponse> {
    return this.apiClient.post<UserResponse>('/users/register', request);
  }

  login(request: LoginRequest): Observable<UserResponse> {
    return this.apiClient.post<UserResponse>('/users/login', request);
  }

  getById(id: string): Observable<UserResponse> {
    return this.apiClient.get<UserResponse>(`/users/${id}`);
  }

  listAll(): Observable<UserResponse[]> {
    return this.apiClient.get<UserResponse[]>('/users');
  }

  updateUser(id: string, request: UpdateUserRequest): Observable<UserResponse> {
    return this.apiClient.put<UserResponse>(`/users/${id}`, request);
  }

  deactivateUser(id: string): Observable<UserResponse> {
    return this.apiClient.put<UserResponse>(`/users/${id}/deactivate`, {});
  }

  activateUser(id: string, password: string): Observable<UserResponse> {
    return this.apiClient.put<UserResponse>(`/users/${id}/activate`, { password });
  }

  validatePassword(id: string, password: string): Observable<void> {
    return this.apiClient.post<void>(`/users/${id}/validate-password`, { password });
  }
}
