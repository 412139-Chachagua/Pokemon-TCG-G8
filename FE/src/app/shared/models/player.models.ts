export interface PlayerResponse {
  id: string;
  displayName: string;
  userId: string;
  createdAt: string;
  avatarUrl?: string | null;
}

export interface UpdatePlayerRequest {
  displayName: string;
}
