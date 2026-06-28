import {
  Component,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { RankingPage } from '../../../ranking/pages/ranking-page/ranking-page';
import { HistoryPage } from '../../../history/pages/history-page/history-page';

export interface MenuCard {
  label: string;
  route: string;
}

@Component({
  selector: 'app-home-page',
  templateUrl: './home-page.html',
  styleUrls: ['./home-page.css'],
  imports: [RankingPage, HistoryPage],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomePage {
  private auth = inject(AuthService);
  private readonly router = inject(Router);

  rankingVisible = signal<boolean>(false);
  historyVisible = signal<boolean>(false);

  displayName = computed(() => this.auth.player()?.displayName ?? 'Entrenador');

  currentHour = signal<number>(new Date().getHours());

  timeOfDay = computed(() => {
    const h = this.currentHour();
    if (h >= 6 && h < 12) return 'morning';
    if (h >= 12 && h < 18) return 'afternoon';
    return 'night';
  });

  greeting = computed(() => {
    const h = this.currentHour();
    if (h >= 6 && h < 12) return 'Buenos días';
    if (h >= 12 && h < 18) return 'Buenas tardes';
    return 'Buenas noches';
  });

  menuCards = signal<MenuCard[]>([
    { label: 'JUGAR', route: '/lobby' },
    { label: 'MIS MAZOS', route: '/decks' },
    { label: 'CATALOGO', route: '/cards' },
    { label: 'REGLAS', route: '/rules' },
  ]);

  constructor() {
    this.currentHour.set(new Date().getHours());
  }

  navigate(route: string): void {
    this.router.navigate([route]);
  }

  openProfile(): void {
    this.router.navigate(['/profile']);
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/auth/login']);
  }

  toggleRanking(): void {
    this.rankingVisible.update(v => !v);
    if (this.rankingVisible()) {
      this.historyVisible.set(false);
    }
  }

  toggleHistory(): void {
    this.historyVisible.update(v => !v);
    if (this.historyVisible()) {
      this.rankingVisible.set(false);
    }
  }
}
