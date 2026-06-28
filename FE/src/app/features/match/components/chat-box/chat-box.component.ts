import { ChangeDetectionStrategy, Component, computed, effect, ElementRef, inject, input, signal, viewChild } from '@angular/core';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { MatchSocketService } from '../../../../core/websocket/match-socket.service';
import { AuthService } from '../../../../core/services/auth.service';
import { AvatarService } from '../../../../core/services/avatar.service';
import { MatchApiService } from '../../../../core/api/match-api.service';
import { PlayerApiService } from '../../../../core/api/player-api.service';
import { RankingApiService } from '../../../../core/api/ranking-api.service';
import { ChatMessage } from '../../../../shared/models/chat.models';

interface ProfileInfo {
  displayName: string;
  createdAt: string;
  totalWins: number;
  totalLosses: number;
}

@Component({
  selector: 'app-chat-box',
  imports: [DragDropModule, FormsModule, DatePipe],
  styles: `
    .chat-messages::-webkit-scrollbar {
      width: 5px;
    }
    .chat-messages::-webkit-scrollbar-track {
      background: transparent;
    }
    .chat-messages::-webkit-scrollbar-thumb {
      background: #475569;
      border-radius: 999px;
    }
    .chat-messages::-webkit-scrollbar-thumb:hover {
      background: #64748b;
    }
  `,
  template: `
    <button
      (click)="toggle()"
      class="fixed bottom-4 left-4 z-50 flex items-center gap-2 px-4 py-2 rounded-full bg-slate-800 border border-slate-700 text-xs font-bold uppercase tracking-wider text-slate-400 cursor-pointer select-none hover:bg-slate-700 transition-colors shadow-lg"
    >
      @if (unreadCount() > 0) {
        <span class="flex items-center justify-center w-5 h-5 rounded-full bg-red-500 text-white text-[10px] font-bold">
          {{ unreadCount() }}
        </span>
      }
      Chat
    </button>

    @if (isOpen()) {
      <div
        cdkDrag
        cdkDragBoundary="app-match-page"
        class="fixed z-50 bg-slate-900 border border-slate-800 shadow-2xl flex flex-col overflow-hidden bottom-0 left-0 sm:bottom-16 sm:left-4 w-full sm:w-[28rem] h-3/5 sm:h-96 min-h-72 sm:min-h-80"
      >
        <div cdkDragHandle class="flex items-center justify-between px-4 py-3 bg-slate-800 border-b border-slate-700 rounded-t-lg cursor-grab active:cursor-grabbing">
          <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Chat</span>
          <button
            (click)="toggle()"
            class="text-slate-400 hover:text-slate-200 cursor-pointer transition-colors text-lg leading-none"
          >✕</button>
        </div>

        <div
          class="chat-messages flex-1 overflow-y-auto pt-1 pb-3 px-3 space-y-3 text-sm"
          #messageContainer
        >
          @for (msg of messages(); track msg.timestamp + msg.senderId) {
            <div
              class="flex gap-2 items-start"
              [class.flex-row-reverse]="msg.senderId === myPlayerId()"
              [class.flex-row]="msg.senderId !== myPlayerId()"
            >
              <div class="w-8 h-8 rounded-full flex-shrink-0 overflow-hidden flex items-center justify-center text-xs font-bold select-none cursor-pointer"
                [style.background-color]="msg.senderId === myPlayerId() ? myAvatarColor() : getAvatarColor(msg.senderId)"
                (click)="openProfile(msg.senderId, $event)"
              >
                @if (getAvatar(msg.senderId); as url) {
                  <img [src]="url" class="w-full h-full object-cover" />
                } @else {
                  <span class="text-white">{{ getInitial(msg.senderId, msg.senderName) }}</span>
                }
              </div>
              <div class="flex flex-col max-w-[75%]" [class.items-end]="msg.senderId === myPlayerId()" [class.items-start]="msg.senderId !== myPlayerId()">
                <span class="text-[10px] text-slate-500 mb-0.5 cursor-pointer hover:underline" (click)="openProfile(msg.senderId, $event)">
                  {{ msg.senderId === myPlayerId() ? 'T\u00fa' : (displayNames()[msg.senderId] ?? msg.senderName) }}
                </span>
                <div
                  class="px-3 py-1.5 rounded-lg break-words leading-relaxed flex flex-col"
                  [class.bg-blue-600]="msg.senderId === myPlayerId()"
                  [class.bg-slate-700]="msg.senderId !== myPlayerId()"
                >
                  <span>{{ msg.content }}</span>
                  <span class="text-[10px] mt-1 self-end opacity-70"
                    [class.text-blue-200]="msg.senderId === myPlayerId()"
                    [class.text-slate-400]="msg.senderId !== myPlayerId()"
                  >{{ msg.timestamp | date:"HH:mm" }}</span>
                </div>
              </div>
            </div>
          } @empty {
            <p class="text-slate-500 text-center py-6 text-sm">No hay mensajes a\u00fan</p>
          }
        </div>

        <div class="relative flex items-center gap-1.5 p-2 border-t border-slate-800">
          @if (showEmoticons()) {
            <div class="absolute bottom-full left-0 mb-1 p-2 bg-slate-800 border border-slate-700 rounded-xl shadow-xl grid grid-cols-5 gap-1.5 z-10 min-w-[240px]">
              @for (emote of emoticons; track emote) {
                <button
                  (click)="insertEmoticon(emote)"
                  class="px-2 py-1 rounded-lg text-sm text-slate-200 hover:bg-slate-700 hover:text-white cursor-pointer select-none transition-colors font-mono"
                >{{ emote }}</button>
              }
            </div>
          }
          <button
            (click)="toggleEmoticons()"
            class="flex items-center justify-center px-2 py-1 rounded-lg bg-slate-700 hover:bg-slate-600 text-slate-300 text-lg cursor-pointer select-none transition-colors leading-none"
            title="Emoticonos"
          >☺</button>
          <input
            [(ngModel)]="newMessage"
            (keyup.enter)="send()"
            placeholder="Escribe un mensaje..."
            class="chat-box-input flex-1 min-w-0 px-2 py-1 rounded-lg bg-slate-800 border border-slate-700 text-sm text-slate-200 placeholder-slate-500 outline-none focus:border-blue-500 transition-colors"
          />
          <button
            (click)="send()"
            [disabled]="!newMessage().trim()"
            class="px-2 py-1 rounded-lg bg-blue-600 text-white text-[11px] font-bold uppercase tracking-wider cursor-pointer select-none hover:bg-blue-500 transition-colors disabled:opacity-40 disabled:cursor-not-allowed whitespace-nowrap"
          >Enviar</button>
        </div>
      </div>
    }

    @if (profileUserId(); as uid) {
      @let info = profileCache()[uid];
      <div class="fixed inset-0 z-[300] backdrop-blur-[2px] flex items-center justify-center" (click)="closeProfile()">
        <div
          class="w-64 bg-gradient-to-b from-slate-800 to-slate-900 border border-slate-600/50 rounded-2xl shadow-2xl overflow-hidden animate-fade-in"
          (click)="$event.stopPropagation()"
        >
          <div class="bg-gradient-to-br from-indigo-600/30 to-purple-700/30 px-5 pt-7 pb-5 flex flex-col items-center border-b border-slate-700/50">
            <div class="w-16 h-16 rounded-full flex-shrink-0 overflow-hidden flex items-center justify-center text-xl font-bold select-none ring-4 ring-slate-700/80 shadow-lg mb-2"
              [style.background-color]="uid === myPlayerId() ? myAvatarColor() : getAvatarColor(uid)"
            >
              @if (getAvatar(uid); as url) {
                <img [src]="url" class="w-full h-full object-cover" />
              } @else {
                <span class="text-white">{{ getInitial(uid, info?.displayName ?? '') }}</span>
              }
            </div>
            <span class="font-bold text-white text-base">{{ info?.displayName ?? 'Cargando...' }}</span>
          </div>
          <div class="p-4 space-y-2.5">
            <div class="flex items-center justify-between px-3 py-2 bg-slate-800/80 rounded-lg">
              <span class="text-xs text-slate-400 uppercase tracking-wider font-semibold">Victorias</span>
              <span class="text-sm font-bold text-green-400">{{ info?.totalWins ?? '—' }}</span>
            </div>
            <div class="flex items-center justify-between px-3 py-2 bg-slate-800/80 rounded-lg">
              <span class="text-xs text-slate-400 uppercase tracking-wider font-semibold">Derrotas</span>
              <span class="text-sm font-bold text-red-400">{{ info?.totalLosses ?? '—' }}</span>
            </div>
            <div class="flex items-center justify-between px-3 py-2 bg-slate-800/80 rounded-lg">
              <span class="text-xs text-slate-400 uppercase tracking-wider font-semibold">Win rate</span>
              <span class="text-sm font-bold text-amber-400">{{ winRate(info) }}</span>
            </div>
            <div class="flex items-center justify-between px-3 py-2 bg-slate-800/80 rounded-lg">
              <span class="text-xs text-slate-400 uppercase tracking-wider font-semibold">Desde</span>
              <span class="text-sm font-bold text-slate-200">{{ info?.createdAt ? (info.createdAt | date:"MM/yyyy") : '—' }}</span>
            </div>
          </div>
        </div>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatBoxComponent {
  readonly matchId = input<string>('');

  private readonly socket = inject(MatchSocketService);
  private readonly auth = inject(AuthService);
  private readonly avatarService = inject(AvatarService);
  private readonly matchApi = inject(MatchApiService);
  private readonly playerApi = inject(PlayerApiService);
  private readonly rankingApi = inject(RankingApiService);
  private readonly messageContainer = viewChild<ElementRef<HTMLElement>>('messageContainer');

  readonly isOpen = signal(false);
  readonly messages = signal<ChatMessage[]>([]);
  readonly newMessage = signal('');
  readonly unreadCount = signal(0);
  readonly showEmoticons = signal(false);
  protected readonly emoticons = [
    ':)', ':(', ':D', ';)', ':P',
    ':o', 'B)', ':*', ":'(", ':/',
    '<3', 'xD', '>:(', ':3', ':))',
  ];

  protected readonly myPlayerId = this.auth.playerId;
  protected readonly myAvatarUrl = computed(() =>
    this.avatarService.resolve(this.auth.player()?.avatarUrl)
  );
  protected readonly myAvatarColor = signal('#3b82f6');

  private readonly avatarUrls = signal<Record<string, string | null>>({});
  protected readonly displayNames = signal<Record<string, string>>({});

  protected readonly profileUserId = signal<string | null>(null);
  protected readonly profileCache = signal<Record<string, ProfileInfo>>({});

  constructor() {
    effect(() => {
      if (this.matchId()) this.loadHistory();
    });

    effect(() => {
      const user = this.auth.user();
      if (user?.displayName && this.myPlayerId()) {
        this.displayNames.update(map => ({ ...map, [this.myPlayerId()!]: user.displayName }));
      }
    });

    this.socket.chatMessages$.subscribe(msg => {
      const prev = this.messages();
      if (
        prev.length > 0 &&
        prev[prev.length - 1].senderId === msg.senderId &&
        prev[prev.length - 1].content === msg.content &&
        Math.abs(prev[prev.length - 1].timestamp - msg.timestamp) < 2000
      ) {
        return;
      }
      this.messages.update(prev => [...prev, msg]);
      if (!this.isOpen()) {
        this.unreadCount.update(c => c + 1);
      }
      this.displayNames.update(map => ({ ...map, [msg.senderId]: msg.senderName }));
      if (msg.senderId !== this.myPlayerId() && !this.avatarUrls()[msg.senderId]) {
        this.fetchAvatar(msg.senderId);
      }
      setTimeout(() => {
        const el = this.messageContainer()?.nativeElement;
        if (el) {
          el.scrollTop = el.scrollHeight;
        }
      });
    });
  }

  protected loadHistory(): void {
    const id = this.matchId();
    if (!id) return;
    this.matchApi.getChatHistory(id).subscribe({
      next: (history) => {
        if (history.length > 0) {
          this.messages.set(history);
          const names: Record<string, string> = {};
          for (const msg of history) {
            if (!names[msg.senderId]) {
              names[msg.senderId] = msg.senderName;
            }
          }
          this.displayNames.update(map => ({ ...map, ...names }));
          setTimeout(() => {
            const el = this.messageContainer()?.nativeElement;
            if (el) el.scrollTop = el.scrollHeight;
          });
        }
      },
    });
  }

  private fetchAvatar(senderId: string): void {
    this.avatarUrls.update(map => ({ ...map, [senderId]: null }));
    this.playerApi.getById(senderId).subscribe({
      next: (player) => {
        this.avatarUrls.update(map => ({
          ...map,
          [senderId]: this.avatarService.resolve(player.avatarUrl),
        }));
        this.displayNames.update(map => ({ ...map, [senderId]: player.displayName }));
      },
    });
  }

  protected getAvatar(senderId: string): string | null {
    if (senderId === this.myPlayerId()) {
      return this.myAvatarUrl();
    }
    return this.avatarUrls()[senderId] ?? null;
  }

  protected getInitial(senderId: string, senderName: string): string {
    return senderName?.charAt(0)?.toUpperCase() || '?';
  }

  protected getAvatarColor(senderId: string): string {
    const colors = ['#6366f1', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#14b8a6', '#f97316', '#ef4444'];
    let hash = 0;
    for (let i = 0; i < senderId.length; i++) {
      hash = senderId.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  protected openProfile(senderId: string, event: MouseEvent): void {
    if (this.profileUserId() === senderId) {
      this.closeProfile();
      return;
    }
    event.stopPropagation();
    this.profileUserId.set(senderId);

    if (!this.profileCache()[senderId]) {
      forkJoin({
        player: this.playerApi.getById(senderId),
        stats: this.rankingApi.getPlayerStats(senderId),
      }).subscribe({
        next: (result) => {
          this.profileCache.update(cache => ({
            ...cache,
            [senderId]: {
              displayName: result.player.displayName,
              createdAt: result.player.createdAt,
              totalWins: result.stats.totalWins,
              totalLosses: result.stats.totalLosses,
            },
          }));
        },
      });
    }
  }

  protected closeProfile(): void {
    this.profileUserId.set(null);
  }

  protected winRate(info: ProfileInfo | undefined): string {
    if (!info || (info.totalWins + info.totalLosses) === 0) return '—';
    return Math.round((info.totalWins / (info.totalWins + info.totalLosses)) * 100) + '%';
  }

  protected toggleEmoticons(): void {
    this.showEmoticons.update(v => !v);
  }

  protected insertEmoticon(emote: string): void {
    this.newMessage.update(msg => msg + emote + ' ');
    this.showEmoticons.set(false);
    const el = document.querySelector<HTMLInputElement>('.chat-box-input');
    el?.focus();
  }

  protected toggle(): void {
    this.isOpen.update(v => !v);
    if (this.isOpen()) {
      this.unreadCount.set(0);
      setTimeout(() => {
        const el = this.messageContainer()?.nativeElement;
        if (el) el.scrollTop = el.scrollHeight;
      });
    } else {
      this.showEmoticons.set(false);
    }
  }

  protected send(): void {
    const content = this.newMessage().trim();
    if (!content) return;
    const playerId = this.myPlayerId();
    if (!playerId) return;
    const senderName = this.auth.user()?.displayName || playerId;
    const msg: ChatMessage = {
      senderId: playerId,
      senderName,
      content,
      timestamp: Date.now(),
    };
    this.socket.sendChatMessage(msg);
    this.messages.update(prev => [...prev, msg]);
    this.displayNames.update(map => ({ ...map, [playerId]: senderName }));
    this.newMessage.set('');
    this.showEmoticons.set(false);
    setTimeout(() => {
      const el = this.messageContainer()?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    });
  }
}
