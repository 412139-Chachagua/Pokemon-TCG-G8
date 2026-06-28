export interface UserResponse {
  id: string;
  email: string;
  displayName: string;
  playerId: string;
  token?: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UpdateUserRequest {
  email?: string;
  currentPassword?: string;
  newPassword?: string;
}
