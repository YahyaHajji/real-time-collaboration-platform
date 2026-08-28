import { PresenceUser } from '../../core/models/collaboration.models';

export function applyPresence(current: PresenceUser[], change: PresenceUser): PresenceUser[] {
  const withoutChangedUser = current.filter((user) => user.userId !== change.userId);
  return change.online
    ? [...withoutChangedUser, change].sort((left, right) => left.displayName.localeCompare(right.displayName))
    : withoutChangedUser;
}
