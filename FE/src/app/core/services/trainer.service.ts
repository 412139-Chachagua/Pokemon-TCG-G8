import { Injectable, signal } from '@angular/core';

const TRAINER_ID_KEY = 'pokemon_trainer_id';
const START_DATE_KEY = 'pokemon_start_date';
const PLAYTIME_KEY = 'pokemon_playtime_ms';

@Injectable({ providedIn: 'root' })
export class TrainerService {
  readonly trainerId = signal<string>(this.resolveTrainerId());
  readonly startDate = signal<string>(this.resolveStartDate());

  private sessionStart = Date.now();
  private savedPlaytime = parseInt(localStorage.getItem(PLAYTIME_KEY) ?? '0', 10);

  get totalPlaytimeMs(): number {
    return this.savedPlaytime + (Date.now() - this.sessionStart);
  }

  savePlaytime(): void {
    localStorage.setItem(PLAYTIME_KEY, String(this.totalPlaytimeMs));
  }

  private resolveTrainerId(): string {
    let id = localStorage.getItem(TRAINER_ID_KEY);
    if (!id) {
      id = String(Math.floor(10000 + Math.random() * 89999));
      localStorage.setItem(TRAINER_ID_KEY, id);
    }
    return id.padStart(5, '0');
  }

  private resolveStartDate(): string {
    let d = localStorage.getItem(START_DATE_KEY);
    if (!d) {
      d = new Date().toLocaleDateString('es-AR');
      localStorage.setItem(START_DATE_KEY, d);
    }
    return d;
  }
}
