import { describe, expect, it } from 'vitest';
import { mergeById } from './merge-by-id';

describe('mergeById', () => {
  it('replaces duplicate logical events and returns deterministic order', () => {
    const result = mergeById(
      [{ id: 'b', value: 1 }, { id: 'a', value: 1 }],
      [{ id: 'b', value: 2 }, { id: 'c', value: 3 }],
      (item) => item.id,
      (left, right) => left.id.localeCompare(right.id)
    );
    expect(result).toEqual([
      { id: 'a', value: 1 },
      { id: 'b', value: 2 },
      { id: 'c', value: 3 }
    ]);
  });
});

