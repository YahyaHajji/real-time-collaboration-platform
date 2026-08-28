import { DestroyRef } from '@angular/core';
import { Subject } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { CollaborationApiService } from '../../core/api/collaboration-api.service';
import { PresenceUser, RealtimeEvent } from '../../core/models/collaboration.models';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { PresenceStore } from './presence.store';

describe('PresenceStore', () => {
  it('replays live deltas received while an older snapshot is in flight', () => {
    const snapshots = new Subject<PresenceUser[]>();
    const presenceEvents = new Subject<RealtimeEvent<PresenceUser>>();
    const connected = new Subject<void>();
    const api = { presence: vi.fn(() => snapshots.asObservable()) };
    const realtime = {
      presence$: presenceEvents.asObservable(),
      connected$: connected.asObservable()
    };

    const store = new PresenceStore(
      api as unknown as CollaborationApiService,
      realtime as unknown as RealtimeService,
      destroyRef()
    );
    const manager: PresenceUser = {
      userId: 2, displayName: 'Maya Manager', role: 'MANAGER', online: true
    };
    const admin: PresenceUser = {
      userId: 1, displayName: 'Amina Admin', role: 'ADMIN', online: true
    };

    store.load();
    presenceEvents.next(event(admin));
    snapshots.next([manager]);

    expect(store.users()).toEqual([admin, manager]);
  });

  it('keeps buffered deltas when a newer snapshot supersedes an in-flight request', () => {
    const firstSnapshot = new Subject<PresenceUser[]>();
    const secondSnapshot = new Subject<PresenceUser[]>();
    const presenceEvents = new Subject<RealtimeEvent<PresenceUser>>();
    const connected = new Subject<void>();
    const api = {
      presence: vi.fn()
        .mockReturnValueOnce(firstSnapshot.asObservable())
        .mockReturnValueOnce(secondSnapshot.asObservable())
    };
    const realtime = {
      presence$: presenceEvents.asObservable(),
      connected$: connected.asObservable()
    };

    const store = new PresenceStore(
      api as unknown as CollaborationApiService,
      realtime as unknown as RealtimeService,
      destroyRef()
    );
    const manager: PresenceUser = {
      userId: 2, displayName: 'Maya Manager', role: 'MANAGER', online: true
    };
    const admin: PresenceUser = {
      userId: 1, displayName: 'Amina Admin', role: 'ADMIN', online: true
    };

    store.load();
    presenceEvents.next(event(admin));
    store.load();
    firstSnapshot.next([manager]);
    secondSnapshot.next([manager]);

    expect(store.users()).toEqual([admin, manager]);
  });
});

function event(payload: PresenceUser): RealtimeEvent<PresenceUser> {
  return {
    eventId: '00000000-0000-7000-8000-000000000001',
    eventType: payload.online ? 'presence.online' : 'presence.offline',
    occurredAt: '2026-08-28T00:00:00Z',
    schemaVersion: 1,
    payload
  };
}

function destroyRef(): DestroyRef {
  return { onDestroy: vi.fn(() => () => undefined) } as unknown as DestroyRef;
}
