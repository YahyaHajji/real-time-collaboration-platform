import { DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CollaborationApiService } from '../../core/api/collaboration-api.service';
import { Channel, ChatMessage } from '../../core/models/collaboration.models';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { mergeById } from '../../shared/merge-by-id';

@Injectable({ providedIn: 'root' })
export class ChatStore {
  private readonly api = inject(CollaborationApiService);
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  readonly channels = signal<Channel[]>([]);
  readonly selected = signal<Channel | null>(null);
  readonly messages = signal<ChatMessage[]>([]);
  readonly loading = signal(false);

  constructor() {
    this.realtime.chat$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => {
        if (event.payload.channelId === this.selected()?.id) {
          this.merge([event.payload]);
          this.markLatestRead();
        }
        this.channels.update((channels) => channels.map((channel) =>
          channel.id === event.payload.channelId && channel.id !== this.selected()?.id
            ? { ...channel, unreadCount: channel.unreadCount + 1 }
            : channel
        ));
      });
    this.realtime.connected$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.loadChannels();
        const selected = this.selected();
        if (selected) {
          this.realtime.subscribeToChat(selected.id);
          this.loadHistory(selected);
        }
      });
  }

  loadChannels(): void {
    this.api.channels().subscribe((channels) => {
      this.channels.set(channels);
      const current = this.selected();
      if (!current && channels.length > 0) this.select(channels[0]);
    });
  }

  select(channel: Channel): void {
    const previous = this.selected();
    if (previous?.id === channel.id) return;
    if (previous) this.realtime.unsubscribeFromChat(previous.id);
    this.selected.set(channel);
    this.messages.set([]);
    this.realtime.subscribeToChat(channel.id);
    this.loadHistory(channel);
  }

  send(body: string): void {
    const channel = this.selected();
    const normalized = body.trim();
    if (!channel || !normalized) return;
    this.realtime.sendChat(channel.id, normalized);
  }

  private loadHistory(channel: Channel): void {
    this.loading.set(true);
    this.api.history(channel.id).subscribe({
      next: (messages) => {
        if (this.selected()?.id === channel.id) {
          this.merge(messages);
          this.markLatestRead();
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  private merge(incoming: ChatMessage[]): void {
    this.messages.update((current) => {
      return mergeById(current, incoming, (message) => message.id,
        (left, right) => left.sentAt.localeCompare(right.sentAt));
    });
  }

  private markLatestRead(): void {
    const channel = this.selected();
    const latest = this.messages().at(-1);
    if (!channel || !latest) return;
    this.api.markChannelRead(channel.id, latest.id).subscribe(() => {
      this.channels.update((channels) => channels.map((item) =>
        item.id === channel.id ? { ...item, unreadCount: 0 } : item
      ));
    });
  }
}
