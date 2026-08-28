import { NotificationItem } from '../../core/models/collaboration.models';

export function countUnread(items: NotificationItem[]): number {
  return items.reduce((count, item) => count + (item.readAt ? 0 : 1), 0);
}
