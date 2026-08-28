import { DestroyRef, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CollaborationApiService } from '../../core/api/collaboration-api.service';
import { PresenceUser } from '../../core/models/collaboration.models';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { applyPresence } from './presence-reducer';

@Injectable({ providedIn: 'root' })
export class PresenceStore {
  private activeSnapshot: { id: number; deltas: PresenceUser[] } | null = null;
  private nextSnapshotId = 0;

  readonly users = signal<PresenceUser[]>([]);

  constructor(
    private readonly api: CollaborationApiService,
    private readonly realtime: RealtimeService,
    destroyRef: DestroyRef
  ) {
    this.realtime.presence$
      .pipe(takeUntilDestroyed(destroyRef))
      .subscribe((event) => {
        this.activeSnapshot?.deltas.push(event.payload);
        this.users.update((current) => applyPresence(current, event.payload));
      });
    this.realtime.connected$
      .pipe(takeUntilDestroyed(destroyRef))
      .subscribe(() => this.load());
  }

  load(): void {
    const request = {
      id: ++this.nextSnapshotId,
      deltas: this.activeSnapshot?.deltas.slice() ?? []
    };
    this.activeSnapshot = request;
    this.api.presence().subscribe({
      next: (users) => {
        if (this.activeSnapshot !== request) return;
        this.users.set(request.deltas.reduce(applyPresence, users));
        this.activeSnapshot = null;
      },
      error: () => {
        if (this.activeSnapshot === request) this.activeSnapshot = null;
      }
    });
  }
}
