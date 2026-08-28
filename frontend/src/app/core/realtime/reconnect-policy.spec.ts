import { describe, expect, it } from 'vitest';
import { reconnectDelay } from './reconnect-policy';

describe('reconnectDelay', () => {
  it('uses exponential growth, bounded at thirty seconds with jitter', () => {
    expect(reconnectDelay(0, 0.5)).toBe(1000);
    expect(reconnectDelay(1, 0.5)).toBe(2000);
    expect(reconnectDelay(5, 0.5)).toBe(30000);
    expect(reconnectDelay(20, 0)).toBe(24000);
    expect(reconnectDelay(20, 1)).toBe(30000);
  });
});
