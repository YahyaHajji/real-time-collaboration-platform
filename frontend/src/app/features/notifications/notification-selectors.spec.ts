import { describe, expect, it } from 'vitest';
import { NotificationItem } from '../../core/models/collaboration.models';
import { countUnread } from './notification-selectors';

describe('countUnread', () => {
  it('counts only notifications without a read timestamp', () => {
    const items = [
      { id: '1', type: 'notice', payload: {}, createdAt: '2026-01-01', readAt: null },
      { id: '2', type: 'notice', payload: {}, createdAt: '2026-01-02', readAt: '2026-01-03' }
    ] satisfies NotificationItem[];
    expect(countUnread(items)).toBe(1);
  });
});
