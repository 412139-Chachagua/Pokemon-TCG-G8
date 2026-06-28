export interface MatchHistoryEntry {
  id: string;
  winnerName: string;
  loserName: string;
  totalTurns: number;
  createdAt: string;
  durationSeconds: number | null;
  finishReason: string | null;
}
