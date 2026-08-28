import { inject, Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import {
  ChatMessage,
  ConnectionState,
  NotificationItem,
  PresenceUser,
  RealtimeEvent
} from '../models/collaboration.models';
import { reconnectDelay } from './reconnect-policy';

@Injectable({ providedIn: 'root' })
export class RealtimeService implements OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly stateSubject = new BehaviorSubject<ConnectionState>('disconnected');
  private readonly connectedSubject = new Subject<void>();
  private readonly notificationSubject = new Subject<RealtimeEvent<NotificationItem>>();
  private readonly presenceSubject = new Subject<RealtimeEvent<PresenceUser>>();
  private readonly chatSubject = new Subject<RealtimeEvent<ChatMessage>>();
  private readonly errorSubject = new Subject<string>();
  private readonly chatSubscriptions = new Map<string, StompSubscription>();
  private client: Client | null = null;
  private reconnectAttempt = 0;

  readonly state$ = this.stateSubject.asObservable();
  readonly connected$ = this.connectedSubject.asObservable();
  readonly notifications$ = this.notificationSubject.asObservable();
  readonly presence$ = this.presenceSubject.asObservable();
  readonly chat$ = this.chatSubject.asObservable();
  readonly errors$ = this.errorSubject.asObservable();

  connect(): void {
    if (this.client?.active) return;
    if (!this.auth.isAuthenticated() || !this.auth.token()) {
      this.auth.logout();
      return;
    }
    this.stateSubject.next('connecting');
    this.client = new Client({
      brokerURL: environment.webSocketUrl,
      connectHeaders: { Authorization: `Bearer ${this.auth.token()}` },
      reconnectDelay: this.nextReconnectDelay(),
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      debug: () => undefined,
      beforeConnect: async () => {
        if (!this.auth.isAuthenticated() || !this.auth.token()) {
          this.auth.logout();
          throw new Error('Authentication expired');
        }
        if (this.client) {
          this.client.connectHeaders = { Authorization: `Bearer ${this.auth.token()}` };
        }
      },
      onConnect: () => {
        this.reconnectAttempt = 0;
        this.stateSubject.next('connected');
        this.subscribeCore();
        this.connectedSubject.next();
      },
      onStompError: () => {
        this.stateSubject.next('error');
        this.errorSubject.next('The real-time session was rejected. Please sign in again.');
        this.auth.logout();
      },
      onWebSocketClose: () => {
        if (this.client?.active) {
          this.stateSubject.next('reconnecting');
          this.client.reconnectDelay = this.nextReconnectDelay();
        } else {
          this.stateSubject.next('disconnected');
        }
      },
      onWebSocketError: () => this.stateSubject.next('error')
    });
    this.client.activate();
  }

  async disconnect(): Promise<void> {
    this.chatSubscriptions.clear();
    if (this.client) await this.client.deactivate();
    this.client = null;
    this.stateSubject.next('disconnected');
  }

  subscribeToChat(channelId: string): void {
    if (!this.client?.connected || this.chatSubscriptions.has(channelId)) return;
    const subscription = this.client.subscribe(`/topic/chat/${channelId}`, (message) => {
      this.chatSubject.next(this.parse<ChatMessage>(message));
    });
    this.chatSubscriptions.set(channelId, subscription);
  }

  unsubscribeFromChat(channelId: string): void {
    this.chatSubscriptions.get(channelId)?.unsubscribe();
    this.chatSubscriptions.delete(channelId);
  }

  sendChat(channelId: string, body: string): void {
    if (!this.client?.connected) throw new Error('Real-time connection is not ready');
    this.client.publish({
      destination: '/app/chat.send',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ channelId, clientMessageId: crypto.randomUUID(), body })
    });
  }

  ngOnDestroy(): void {
    void this.disconnect();
  }

  private subscribeCore(): void {
    if (!this.client) return;
    this.chatSubscriptions.clear();
    this.client.subscribe('/user/queue/notifications', (message) => {
      this.notificationSubject.next(this.parse<NotificationItem>(message));
    });
    this.client.subscribe('/topic/presence', (message) => {
      this.presenceSubject.next(this.parse<PresenceUser>(message));
    });
    this.client.subscribe('/user/queue/errors', (message) => {
      const error = JSON.parse(message.body) as { message?: string };
      this.errorSubject.next(error.message ?? 'A real-time command was rejected');
    });
  }

  private parse<T>(message: IMessage): RealtimeEvent<T> {
    return JSON.parse(message.body) as RealtimeEvent<T>;
  }

  private nextReconnectDelay(): number {
    return reconnectDelay(this.reconnectAttempt++);
  }
}
