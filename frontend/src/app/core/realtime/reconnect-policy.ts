export function reconnectDelay(attempt: number, random = Math.random()): number {
  const base = Math.min(30_000, 1000 * 2 ** Math.max(0, attempt));
  return Math.min(30_000, Math.round(base * (0.8 + Math.min(1, Math.max(0, random)) * 0.4)));
}
