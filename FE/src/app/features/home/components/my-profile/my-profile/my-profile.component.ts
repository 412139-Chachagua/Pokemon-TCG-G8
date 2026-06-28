import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  computed,
  effect,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TrainerService } from '../../../../../core/services/trainer.service';
import { AuthService } from '../../../../../core/services/auth.service';
import { AvatarService } from '../../../../../core/services/avatar.service';

@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-profile.component.html',
  styleUrls: ['./my-profile.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyProfileComponent implements OnInit, OnDestroy {
  private trainer = inject(TrainerService);
  private auth = inject(AuthService);
  private avatarService = inject(AvatarService);

  trainerId = this.trainer.trainerId;
  startDate = this.trainer.startDate;
  playerName = signal<string>(this.auth.player()?.displayName ?? 'Entrenador');

  readonly player = this.auth.player;

  readonly avatarUrl = computed(() =>
    this.avatarService.resolve(this.player()?.avatarUrl)
  );

  readonly avatarInitials = computed(() => {
    const name = this.player()?.displayName ?? '';
    return name
      .split(' ')
      .filter(p => p.length > 0)
      .map(p => p[0].toUpperCase())
      .slice(0, 2)
      .join('');
  });

  readonly avatarLoadError = signal(false);

  playtime = signal<string>('00:00:00');

  private interval?: ReturnType<typeof setInterval>;

  constructor() {
    effect(() => {
      this.avatarUrl();
      this.avatarLoadError.set(false);
    });
  }

  ngOnInit(): void {
    this.tick();
    this.interval = setInterval(() => this.tick(), 1000);
  }

  ngOnDestroy(): void {
    clearInterval(this.interval);
    this.trainer.savePlaytime();
  }

  onAvatarError(): void {
    this.avatarLoadError.set(true);
  }

  private tick(): void {
    const ms = this.trainer.totalPlaytimeMs;
    const secs = Math.floor(ms / 1000);
    const h = Math.floor(secs / 3600);
    const m = Math.floor(secs / 60) % 60;
    const s = secs % 60;
    this.playtime.set(
      String(h).padStart(2, '0') + ':' +
      String(m).padStart(2, '0') + ':' +
      String(s).padStart(2, '0')
    );
  }
}