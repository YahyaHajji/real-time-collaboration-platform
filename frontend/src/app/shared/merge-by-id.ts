export function mergeById<T>(
  current: readonly T[],
  incoming: readonly T[],
  id: (value: T) => string,
  compare: (left: T, right: T) => number
): T[] {
  const values = new Map(current.map((value) => [id(value), value]));
  incoming.forEach((value) => values.set(id(value), value));
  return [...values.values()].sort(compare);
}

