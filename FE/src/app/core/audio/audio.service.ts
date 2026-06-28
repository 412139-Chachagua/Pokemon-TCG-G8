import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

type AudioCategory = 'splash' | 'general' | 'board' | 'silent';

interface TrackConfig {
  src: string;
  loop: boolean;
}

const TRACK_MAP: Record<AudioCategory, TrackConfig | null> = {
  splash: { src: 'assets/audio/title-screen-song.mp3', loop: true },
  general: { src: 'assets/audio/menu-ost_A.mp3', loop: true },
  board: { src: 'assets/audio/board-song.mp3', loop: true },
  silent: null,
};

@Injectable({ providedIn: 'root' })
export class AudioService {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private audio: HTMLAudioElement | null = null;
  private currentCategory: AudioCategory = 'silent';
  private userInteracted = false;

  private readonly STORAGE_KEY_GENERAL = 'pokemon_general_volume';
  private readonly STORAGE_KEY_BOARD = 'pokemon_board_volume';
  private readonly STORAGE_KEY_SFX = 'pokemon_sfx_volume';

  readonly isPlaying = signal(false);
  readonly generalVolume = signal(100);
  readonly boardVolume = signal(40);
  readonly sfxVolume = signal(40);

  constructor() {
    this.generalVolume.set(this.loadVolume(this.STORAGE_KEY_GENERAL, 100));
    this.boardVolume.set(this.loadVolume(this.STORAGE_KEY_BOARD, 60));
    this.sfxVolume.set(this.loadVolume(this.STORAGE_KEY_SFX, 40));

    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((event) => this.onRouteChanged(event.urlAfterRedirects));

    this.setupAutoplayHandler();
    this.setupSfxHandler();
  }

  private loadVolume(key: string, fallback: number): number {
    const stored = localStorage.getItem(key);
    if (stored === null) return fallback;
    const parsed = Number(stored);
    return isNaN(parsed) ? fallback : Math.max(0, Math.min(100, parsed));
  }

  setGeneralVolume(vol: number): void {
    const clamped = Math.max(0, Math.min(100, vol));
    this.generalVolume.set(clamped);
    localStorage.setItem(this.STORAGE_KEY_GENERAL, String(clamped));
    if (this.currentCategory === 'general' && this.audio) {
      this.audio.volume = clamped / 100;
    }
  }

  setBoardVolume(vol: number): void {
    const clamped = Math.max(0, Math.min(100, vol));
    this.boardVolume.set(clamped);
    localStorage.setItem(this.STORAGE_KEY_BOARD, String(clamped));
    if (this.currentCategory === 'board' && this.audio) {
      this.audio.volume = clamped / 100;
    }
  }

  setSfxVolume(vol: number): void {
    const clamped = Math.max(0, Math.min(100, vol));
    this.sfxVolume.set(clamped);
    localStorage.setItem(this.STORAGE_KEY_SFX, String(clamped));
  }

  private onRouteChanged(url: string): void {
    this.playCategory(this.categorizeRoute(url));
  }

  private categorizeRoute(url: string): AudioCategory {
    if (url === '/welcome') return 'splash';
    if (url.startsWith('/match/')) return 'board';
    if (
      url.startsWith('/auth/login') ||
      url.startsWith('/auth/register') ||
      url.startsWith('/sandbox')
    ) {
      return 'silent';
    }
    return 'general';
  }

  private playCategory(category: AudioCategory): void {
    if (category === this.currentCategory) return;

    this.stop();
    this.currentCategory = category;

    const config = TRACK_MAP[category];
    if (!config) return;

    const audio = new Audio(config.src);
    audio.loop = config.loop;
    if (category === 'general') {
      audio.volume = this.generalVolume() / 100;
    } else if (category === 'board') {
      audio.volume = this.boardVolume() / 100;
    } else {
      audio.volume = 0.2;
    }
    this.audio = audio;

    if (this.userInteracted) {
      this.tryPlay();
    }
  }

  private stop(): void {
    if (this.audio) {
      this.audio.pause();
      this.audio.src = '';
      this.audio.load();
      this.audio = null;
    }
    this.isPlaying.set(false);
  }

  private tryPlay(): void {
    if (!this.audio) return;
    this.audio.play().then(() => this.isPlaying.set(true)).catch(() => {});
  }

  private setupAutoplayHandler(): void {
    const onInteraction = () => {
      if (this.userInteracted) return;
      this.userInteracted = true;
      if (this.audio && this.audio.paused) {
        this.tryPlay();
      }
      document.removeEventListener('click', onInteraction);
      document.removeEventListener('keydown', onInteraction);
    };
    document.addEventListener('click', onInteraction);
    document.addEventListener('keydown', onInteraction);
  }

  private sfx: HTMLAudioElement | null = null;

  private setupSfxHandler(): void {
    this.sfx = new Audio('assets/audio/click-sound.mp3');
    this.sfx.preload = 'auto';
    this.sfx.volume = 1.0;

    document.addEventListener('click', () => {
      this.playSfx();
    });
  }

  playSfx(): void {
    if (!this.sfx) return;
    this.sfx.currentTime = 0;
    this.sfx.play().catch((err) => console.warn('[Audio] click-sound error:', err));
  }

  private readonly TYPE_SOUND_MAP: Record<string, string> = {
    WATER: 'assets/audio/water.wav',
    LIGHTNING: 'assets/audio/electric.wav',
    PSYCHIC: 'assets/audio/psychic.wav',
    FIGHTING: 'assets/audio/fighting.wav',
    DARKNESS: 'assets/audio/dark.wav',
    FAIRY: 'assets/audio/fairy.wav',
    GRASS: 'assets/audio/plant.wav',
    FIRE: 'assets/audio/fire.wav',
    COLORLESS: 'assets/audio/colorless.wav',
    METAL: 'assets/audio/metallic.wav',
  };

  playTypeSound(type: string): void {
    const src = this.TYPE_SOUND_MAP[type?.toUpperCase()];
    if (!src) return;
    this.playSound(src);
  }

  playEvolutionSound(): void {
    this.playSound('assets/audio/evolution.wav');
  }

  playActivePokemonSound(): void {
    this.playSound('assets/audio/active-pokemon.mp3', 0.25);
  }

  playFirstStartSound(): void {
    this.playSound('assets/audio/first-start.mp3', 0.8);
  }

  playKoSound(): void {
    this.playSound('assets/audio/pokemon-ko.mp3');
  }

  private energyAttachIndex = 0;

  playEnergyAttachSound(): void {
    const files = ['assets/audio/energy-attach.wav', 'assets/audio/energy-attach1.wav'];
    const src = files[this.energyAttachIndex];
    this.energyAttachIndex = 1 - this.energyAttachIndex;
    this.playSound(src);
  }

  private benchIndex = 0;

  playBenchSound(): void {
    const files = [
      'assets/audio/bench.wav',
      'assets/audio/bench1.wav',
      'assets/audio/bench2.wav',
      'assets/audio/bench3.wav',
      'assets/audio/bench4.wav',
    ];
    const src = files[this.benchIndex];
    this.benchIndex = (this.benchIndex + 1) % files.length;
    this.playSound(src);
  }

  private playSound(src: string, volumeOverride?: number): void {
    const audio = new Audio(src);
    audio.volume = volumeOverride ?? this.sfxVolume() / 100;
    audio.play().catch(() => {});
  }
}
