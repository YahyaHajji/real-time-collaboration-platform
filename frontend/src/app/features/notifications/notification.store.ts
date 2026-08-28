import { DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CollaborationApiService } from '../../core/api/collaboration-api.service';
import { NotificationItem } from '../../core/models/collaboration.models';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { mergeById } from '../../shared/merge-by-id';
import { countUnread } from './notification-selectors';

@Injectable({ providedIn: 'root' })
export class NotificationStore {
  private readonly api = inject(CollaborationApiService);
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);
  private cursor: string | undefined;

  readonly items = signal<NotificationItem[]>([]);
  readonly unreadCount = signal(0);
  readonly loading = signal(false);

  constructor() {
    this.realtime.notifications$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => this.merge([event.payload]));
    this.realtime.connected$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load(this.items().length > 0));
  }

  load(incremental = false): void {
    this.loading.set(true);
    this.api.notifications(incremental ? this.cursor : undefined).subscribe({
      next: (page) => {
        this.merge(page.items);
        this.unreadCount.set(page.unreadCount);
        this.cursor = page.nextCursor;
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  markRead(item: NotificationItem): void {
    if (item.readAt) return;
    this.api.markNotificationRead(item.id).subscribe((updated) => {
      this.items.update((items) => items.map((value) => value.id === updated.id ? updated : value));
      this.unreadCount.update((count) => Math.max(0, count - 1));
    });
  }

  private merge(incoming: NotificationItem[]): void {
    this.items.update((current) => {
      return mergeById(current, incoming, (item) => item.id,
        (left, right) => right.createdAt.localeCompare(left.createdAt));
    });
    this.unreadCount.set(countUnread(this.items()));
    const latest = this.items().reduce<string | undefined>(
      (cursor, item) => !cursor || item.createdAt > cursor ? item.createdAt : cursor,
      this.cursor
    );
    this.cursor = latest;
  }
}
