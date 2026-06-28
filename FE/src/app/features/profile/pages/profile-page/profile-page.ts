import { ChangeDetectionStrategy, Component, computed, effect, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, Validators, FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { AudioService } from '../../../../core/audio/audio.service';
import { TrainerService } from '../../../../core/services/trainer.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { AvatarService } from '../../../../core/services/avatar.service';
import { RankingApiService } from '../../../../core/api/ranking-api.service';
import { DeckApiService } from '../../../../core/api/deck-api.service';
import { PlayerStats } from '../../../../shared/models/ranking.models';
import { AvatarPickerDialogComponent } from '../../components/avatar-picker-dialog/avatar-picker-dialog.component';
import { BackButtonComponent } from '../../../../shared/components/back-button/back-button.component';

@Component({
  selector: 'app-profile-page',
  imports: [CommonModule, ReactiveFormsModule, AvatarPickerDialogComponent, BackButtonComponent],
  templateUrl: './profile-page.html',
  styleUrls: ['./profile-page.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePage implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly trainerService = inject(TrainerService);
  private readonly notificationService = inject(NotificationService);
  private readonly avatarService = inject(AvatarService);
  private readonly router = inject(Router);
  private readonly rankingApi = inject(RankingApiService);
  private readonly deckApi = inject(DeckApiService);
  protected readonly audioService = inject(AudioService);

  readonly user = this.authService.user;
  readonly player = this.authService.player;
  readonly playerId = this.authService.playerId;

  readonly trainerId = this.trainerService.trainerId;
  readonly startDate = this.trainerService.startDate;

  readonly avatarUrl = computed(() =>
    this.avatarService.resolve(this.player()?.avatarUrl)
  );

  readonly avatarInitials = computed(() => {
    const name = this.player()?.displayName ?? '';
    return name
      .split(' ')
      .filter(part => part.length > 0)
      .map(part => part[0].toUpperCase())
      .slice(0, 2)
      .join('');
  });

  readonly editMode = signal(false);
  readonly avatarLoadError = signal(false);
  readonly showAvatarPicker = signal(false);

  constructor() {
    effect(() => {
      this.avatarUrl();
      this.avatarLoadError.set(false);
    });
  }

  onAvatarError(): void {
    this.avatarLoadError.set(true);
  }

  openAvatarPicker(): void {
    this.showAvatarPicker.set(true);
  }

  closeAvatarPicker(): void {
    this.showAvatarPicker.set(false);
  }

  onAvatarUploaded(): void {
    this.notificationService.show('Avatar actualizado correctamente', 'success');
    this.closeAvatarPicker();
  }

  readonly stats = signal<PlayerStats | null>(null);
  readonly statsError = signal<string | null>(null);
  readonly deckCount = signal(0);
  readonly deckCountError = signal<string | null>(null);
  readonly playerRank = signal<number | null>(null);
  readonly rankingError = signal<string | null>(null);

  readonly winRate = computed(() => {
    const s = this.stats();
    if (!s) return null;
    const total = s.totalWins + s.totalLosses;
    if (total === 0) return 0;
    return Math.round((s.totalWins / total) * 100);
  });

  readonly playtime = signal('00:00:00');
  private interval?: ReturnType<typeof setInterval>;

  readonly emailControl = new FormControl('', [Validators.required, Validators.email]);
  readonly displayNameControl = new FormControl('', [
    Validators.required,
    Validators.minLength(2),
    Validators.maxLength(30),
  ]);

  readonly passwordForm = new FormGroup({
    currentPassword: new FormControl('', [Validators.required]),
    newPassword: new FormControl('', [Validators.required, Validators.minLength(6)]),
    confirmPassword: new FormControl('', [Validators.required]),
  });

  readonly deleteForm = new FormGroup({
    password: new FormControl('', [Validators.required]),
  });

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showPasswordModal = signal(false);
  readonly passwordLoading = signal(false);
  readonly passwordError = signal<string | null>(null);
  readonly showDeleteModal = signal(false);
  readonly deleteLoading = signal(false);
  readonly deleteError = signal<string | null>(null);

  ngOnInit(): void {
    this.emailControl.setValue(this.user()?.email ?? '');
    this.displayNameControl.setValue(this.player()?.displayName ?? '');
    this.tick();
    this.interval = setInterval(() => this.tick(), 1000);
    this.loadStats();
  }

  ngOnDestroy(): void {
    clearInterval(this.interval);
    this.trainerService.savePlaytime();
  }

  private tick(): void {
    const ms = this.trainerService.totalPlaytimeMs;
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

  private loadStats(): void {
    const playerId = this.authService.playerId();
    if (!playerId) return;

    this.rankingApi.getPlayerStats(playerId).subscribe({
      next: (stats) => this.stats.set(stats),
      error: () => this.statsError.set('No disponible'),
    });

    this.rankingApi.getRanking().subscribe({
      next: (ranking) => {
        const entry = ranking.find(e => e.playerId === playerId);
        this.playerRank.set(entry?.rank ?? null);
      },
      error: () => this.rankingError.set('No disponible'),
    });

    this.deckApi.listByPlayer(playerId).subscribe({
      next: (decks) => this.deckCount.set(decks.length),
      error: () => this.deckCountError.set('No disponible'),
    });
  }

  enterEditMode(): void {
    this.emailControl.setValue(this.user()?.email ?? '');
    this.displayNameControl.setValue(this.player()?.displayName ?? '');
    this.errorMessage.set(null);
    this.editMode.set(true);
  }

  exitEditMode(): void {
    this.editMode.set(false);
    this.errorMessage.set(null);
    this.emailControl.reset();
    this.displayNameControl.reset();
  }

  onSave(): void {
    if (this.displayNameControl.invalid) {
      this.displayNameControl.markAsTouched();
      return;
    }

    if (this.emailControl.invalid) {
      this.emailControl.markAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const playerId = this.authService.playerId();
    const userId = this.user()?.id;

    if (!playerId || !userId) {
      this.errorMessage.set('No se encontró la información del usuario');
      this.loading.set(false);
      return;
    }

    const emailChanged = this.emailControl.value !== (this.user()?.email ?? '');
    const nameChanged = this.displayNameControl.value !== (this.player()?.displayName ?? '');

    const requests: Observable<unknown>[] = [];

    if (nameChanged) {
      requests.push(this.authService.updatePlayer(playerId, this.displayNameControl.value!));
    }

    if (emailChanged) {
      requests.push(this.authService.updateUser(userId, { email: this.emailControl.value! }));
    }

    if (requests.length === 0) {
      this.notificationService.show('No hay cambios para guardar', 'success');
      this.loading.set(false);
      return;
    }

    if (requests.length === 1) {
      requests[0].subscribe({
        next: () => {
          this.notificationService.show('Perfil actualizado correctamente', 'success');
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          const message = err.error?.message || 'Error al actualizar el perfil';
          this.errorMessage.set(message);
        },
      });
    } else {
      this.authService.updatePlayer(playerId, this.displayNameControl.value!).subscribe({
        next: () => {
          this.authService.updateUser(userId, { email: this.emailControl.value! }).subscribe({
            next: () => {
              this.notificationService.show('Perfil actualizado correctamente', 'success');
              this.loading.set(false);
            },
            error: (err) => {
              this.loading.set(false);
              const message = err.error?.message || 'Error al actualizar el email';
              this.errorMessage.set(message);
            },
          });
        },
        error: (err) => {
          this.loading.set(false);
          const message = err.error?.message || 'Error al actualizar el nombre';
          this.errorMessage.set(message);
        },
      });
    }
  }

  onCancel(): void {
    this.exitEditMode();
  }

  onMenuVolumeChange(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    this.audioService.setGeneralVolume(value);
  }

  onBoardVolumeChange(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    this.audioService.setBoardVolume(value);
  }

goBack(): void {
    this.router.navigate(['/home'], { queryParams: { hub: true } });
}

  openPasswordModal(): void {
    this.passwordForm.reset();
    this.passwordError.set(null);
    this.showPasswordModal.set(true);
  }

  closePasswordModal(): void {
    this.showPasswordModal.set(false);
    this.passwordForm.reset();
    this.passwordError.set(null);
  }

  onChangePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const { currentPassword, newPassword, confirmPassword } = this.passwordForm.value;

    if (newPassword !== confirmPassword) {
      this.passwordError.set('Las contraseñas no coinciden');
      return;
    }

    this.passwordLoading.set(true);
    this.passwordError.set(null);

    const userId = this.user()?.id;
    if (!userId) {
      this.passwordError.set('No se encontró el ID del usuario');
      this.passwordLoading.set(false);
      return;
    }

    this.authService
      .updateUser(userId, { currentPassword: currentPassword!, newPassword: newPassword! })
      .subscribe({
        next: () => {
          this.notificationService.show('Contraseña cambiada correctamente', 'success');
          this.passwordLoading.set(false);
          this.closePasswordModal();
        },
        error: (err) => {
          this.passwordLoading.set(false);
          const message = err.error?.message || 'Error al cambiar la contraseña';
          this.passwordError.set(message);
        },
      });
  }

  openDeleteModal(): void {
    this.deleteForm.reset();
    this.deleteError.set(null);
    this.showDeleteModal.set(true);
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
    this.deleteForm.reset();
    this.deleteError.set(null);
  }

  onDeleteAccount(): void {
    if (this.deleteForm.invalid) {
      this.deleteForm.markAllAsTouched();
      return;
    }

    this.deleteLoading.set(true);
    this.deleteError.set(null);

    const userId = this.user()?.id;
    const password = this.deleteForm.value.password;

    if (!userId) {
      this.deleteError.set('No se encontró el ID del usuario');
      this.deleteLoading.set(false);
      return;
    }

    this.authService.validatePassword(userId, password!).subscribe({
      next: () => {
        this.authService.deactivateUser(userId).subscribe({
          next: () => {
            this.notificationService.show('Cuenta eliminada correctamente', 'success');
            this.deleteLoading.set(false);
            this.closeDeleteModal();
            this.router.navigate(['/auth/login']);
          },
          error: (err) => {
            this.deleteLoading.set(false);
            const message = err.error?.message || 'Error al eliminar la cuenta';
            this.deleteError.set(message);
          },
        });
      },
      error: (err) => {
        this.deleteLoading.set(false);
        const message = err.error?.message || 'Contraseña incorrecta';
        this.deleteError.set(message);
      },
    });
  }
}
