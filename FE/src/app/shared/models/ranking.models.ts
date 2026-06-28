export interface RankingEntry {
  rank: number;
  playerId: string;
  displayName: string;
  totalWins: number;
  totalLosses: number;
  winRate: number;
  currentWinStreak: number;
  maxWinStreak: number;
}

export interface PlayerStats {
  playerId: string;
  displayName: string;
  totalWins: number;
  totalLosses: number;
  currentWinStreak: number;
  maxWinStreak: number;
}
