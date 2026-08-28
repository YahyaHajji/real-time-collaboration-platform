import { Role } from '../auth/auth.models';

export interface RealtimeEvent<T> {
  eventId: string;
  eventType: string;
  occurredAt: string;
  schemaVersion: number;
  payload: T;
}

export interface NotificationItem {
  id: string;
  type: string;
  payload: { message?: string; [key: string]: unknown };
  createdAt: string;
  readAt: string | null;
}

export interface NotificationPage {
  items: NotificationItem[];
  unreadCount: number;
  nextCursor: string;
}

export interface UserSummary {
  id: number;
  email: string;
  displayName: string;
  role: Role;
}

export interface Channel {
  id: string;
  name: string;
  requiredRole: Role | null;
  unreadCount: number;
}

export interface ChatMessage {
  id: string;
  clientMessageId: string;
  channelId: string;
  senderId: number;
  senderName: string;
  senderRole: Role;
  body: string;
  sentAt: string;
}

export interface PresenceUser {
  userId: number;
  displayName: string;
  role: Role;
  online: boolean;
}

export type ConnectionState = 'disconnected' | 'connecting' | 'connected' | 'reconnecting' | 'error';

