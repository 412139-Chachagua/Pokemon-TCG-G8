import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  input,
  OnDestroy,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { finalize } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';

const ALLOWED_EXTENSIONS = ['.png', '.jpg', '.jpeg', '.webp'];
const ALLOWED_MIMES = ['image/png', 'image/jpeg', 'image/webp'];
const MAX_SIZE_BYTES = 2 * 1024 * 1024;

@Component({
  selector: 'app-avatar-picker-dialog',
  template: `
    <div class="dialog-overlay" (click)="onCancel()">
      <div class="dialog-content" (click)="$event.stopPropagation()">
        <h3>Elegir avatar</h3>

        <div class="avatar-preview">
          @if (currentImage()) {
            <img [src]="currentImage()" [alt]="displayName()" />
          } @else {
            <div class="avatar-placeholder">
              <span>{{ avatarInitials() }}</span>
            </div>
          }
        </div>

        <input
          #fileInput
          type="file"
          class="file-input"
          accept=".png,.jpg,.jpeg,.webp,image/png,image/jpeg,image/webp"
          (change)="onFileSelected($event)"
        />
        <button class="pk-btn" (click)="triggerFileInput()">Seleccionar archivo</button>

        @if (selectedFile()) {
          <p class="file-name">{{ selectedFile()!.name }}</p>
        }

        @if (validationError()) {
          <p class="error-msg">{{ validationError() }}</p>
        }
        @if (uploadError()) {
          <p class="error-msg">{{ uploadError() }}</p>
        }

        <div class="dialog-actions">
          @if (selectedFile()) {
            <button
              class="pk-accent-btn"
              [disabled]="uploading()"
              (click)="onUpload()"
            >
              @if (uploading()) {
                Subiendo...
              } @else {
                Subir avatar
              }
            </button>
          }
          <button class="pk-btn" [disabled]="uploading()" (click)="onCancel()">
            Cancelar
          </button>
        </div>
      </div>
    </div>
  `,
  styles: `
    .dialog-overlay {
      position: fixed;
      inset: 0;
      z-index: 50;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.5);
    }

    .dialog-content {
      background: var(--pk-surface);
      border: 2px solid var(--pk-accent);
      border-radius: 8px;
      padding: 1.5rem;
      max-width: 24rem;
      width: 90%;
      text-align: center;
    }

    h3 {
      font-family: 'Press Start 2P', system-ui, sans-serif;
      font-size: clamp(0.65rem, 1.5vw, 0.85rem);
      color: var(--pk-text-bright);
      margin: 0 0 1rem;
    }

    .avatar-preview {
      width: 240px;
      height: 240px;
      border-radius: 50%;
      border: 2px solid var(--pk-accent);
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--pk-panel);
      margin: 0 auto 1rem;
    }

    .avatar-preview img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .avatar-placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 100%;
      background: var(--pk-panel);
    }

    .avatar-placeholder span {
      font-family: 'Press Start 2P', system-ui, sans-serif;
      font-size: clamp(1.5rem, 4vw, 2.2rem);
      color: var(--pk-text-bright);
      text-shadow: 0 0 12px rgba(155, 109, 255, 0.5);
    }

    .file-input {
      display: none;
    }

    .file-name {
      font-size: 0.7rem;
      color: var(--pk-text-dim);
      margin: 0.5rem 0 0;
      word-break: break-all;
    }

    .error-msg {
      font-size: 0.7rem;
      color: #e74c3c;
      margin: 0.5rem 0 0;
    }

    .dialog-actions {
      display: flex;
      gap: 0.75rem;
      justify-content: center;
      margin-top: 1rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AvatarPickerDialogComponent implements OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly playerId = input.required<string>();
  readonly avatarUrl = input<string | null>(null);
  readonly displayName = input<string>('');

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  readonly selectedFile = signal<File | null>(null);
  readonly previewUrl = signal<string | null>(null);
  readonly validationError = signal<string | null>(null);
  readonly uploadError = signal<string | null>(null);
  readonly uploading = signal(false);

  readonly fileInput = viewChild.required<ElementRef<HTMLInputElement>>('fileInput');

  readonly avatarInitials = computed(() =>
    this.displayName()
      .split(' ')
      .filter(p => p.length > 0)
      .map(p => p[0].toUpperCase())
      .slice(0, 2)
      .join('')
  );

  readonly currentImage = computed(() => this.previewUrl() ?? this.avatarUrl());

  constructor() {
    this.destroyRef.onDestroy(() => this.cleanup());
  }

  ngOnDestroy(): void {
    this.cleanup();
  }

  triggerFileInput(): void {
    this.fileInput().nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const ext = '.' + file.name.split('.').pop()?.toLowerCase();
    if (!ALLOWED_EXTENSIONS.includes(ext) || !ALLOWED_MIMES.includes(file.type)) {
      this.validationError.set('Tipo de archivo no permitido. Use PNG, JPG o WEBP');
      return;
    }

    if (file.size > MAX_SIZE_BYTES) {
      this.validationError.set('El archivo excede el tamaño máximo de 2 MB');
      return;
    }

    this.cleanup();
    this.previewUrl.set(URL.createObjectURL(file));
    this.selectedFile.set(file);
  }

  onUpload(): void {
    if (this.uploading()) return;

    const file = this.selectedFile();
    if (!file) return;

    this.uploading.set(true);
    this.uploadError.set(null);
    this.validationError.set(null);

    this.authService
      .updateAvatar(this.playerId(), file)
      .pipe(finalize(() => this.uploading.set(false)))
      .subscribe({
        next: () => {
          this.cleanup();
          this.confirmed.emit();
        },
        error: (err) => {
          const status = err.status;
          if (status === 413) {
            this.uploadError.set('El archivo excede el tamaño máximo de 2 MB');
          } else if (status === 415) {
            this.uploadError.set('Tipo de archivo no permitido. Use PNG, JPG o WEBP');
          } else {
            this.uploadError.set('No fue posible subir el avatar. Intentá de nuevo.');
          }
        },
      });
  }

  onCancel(): void {
    this.cleanup();
    this.cancelled.emit();
  }

  private cleanup(): void {
    const url = this.previewUrl();
    if (url) URL.revokeObjectURL(url);

    this.previewUrl.set(null);
    this.selectedFile.set(null);
    this.validationError.set(null);
    this.uploadError.set(null);

    const input = this.fileInput();
    if (input) input.nativeElement.value = '';
  }
}
