import { describe, expect, it } from 'vitest';
import { applyPresence } from './presence-reducer';

describe('applyPresence', () => {
  it('upserts online users and removes final offline users', () => {
    const online = { userId: 3, displayName: 'Youssef User', role: 'USER' as const, online: true };
    expect(applyPresence([], online)).toEqual([online]);
    expect(applyPresence([online], { ...online, online: false })).toEqual([]);
  });
});
