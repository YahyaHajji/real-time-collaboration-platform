import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { CollaborationApiService } from '../../core/api/collaboration-api.service';
import { Channel, ConnectionState, UserSummary } from '../../core/models/collaboration.models';
import { RealtimeService } from '../../core/realtime/realtime.service';
import { ChatStore } from '../chat/chat.store';
import { NotificationStore } from '../notifications/notification.store';
import { PresenceStore } from '../presence/presence.store';

type WorkspacePane = 'chat' | 'activity' | 'people';

@Component({
  selector: 'app-workspace',
  imports: [AsyncPipe, DatePipe, FormsModule],
  template: `
    <div class="relay-app" [class.activity-collapsed]="!activityOpen()">
      @if (error()) {
        <div class="global-alert" role="alert">
          <span>{{ error() }}</span>
          <button type="button" (click)="error.set('')">Dismiss</button>
        </div>
      }

      <header class="mobile-appbar">
        <span class="logo-tile compact"><img src="relay-logo.svg" alt=""></span>
        <strong>Relay</strong>
        @if (realtime.state$ | async; as state) {
          <span class="connection-pill" [class.connected]="state === 'connected'"
                [class.offline]="state === 'disconnected' || state === 'error'">
            <i></i>{{ connectionLabel(state) }}
          </span>
        }
      </header>

      <aside class="workspace-sidebar" aria-label="Workspace navigation">
        <div class="sidebar-brand relay-brand">
          <span class="logo-tile"><img src="relay-logo.svg" alt=""></span>
          <span><strong>Relay</strong><small>Real-time collaboration</small></span>
        </div>

        <div class="workspace-label">
          <strong>Team workspace</strong>
          <span>Live collaboration</span>
        </div>

        <nav class="sidebar-navigation">
          <section class="nav-section" aria-labelledby="workspace-nav-title">
            <div class="nav-heading" id="workspace-nav-title">Workspace</div>
            <button class="nav-item" type="button" aria-label="Notifications" title="Notifications" (click)="showActivity()"
                    [class.selected]="mobilePane() === 'activity'">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 5h16v12H7l-3 3z"/></svg>
              <span>Notifications</span>
              @if (notifications.unreadCount()) { <b class="nav-badge">{{ notifications.unreadCount() }}</b> }
            </button>
          </section>

          <section class="nav-section" aria-labelledby="channels-nav-title">
            <div class="nav-heading" id="channels-nav-title"><span>Channels</span><span>{{ chat.channels().length }}</span></div>
            @for (channel of chat.channels(); track channel.id) {
              <button class="nav-item channel-item" type="button"
                      [attr.aria-label]="'Channel ' + channel.name + (channel.unreadCount ? ', ' + channel.unreadCount + ' unread' : '')"
                      [attr.title]="'# ' + channel.name"
                      [class.selected]="chat.selected()?.id === channel.id && mobilePane() === 'chat'"
                      [attr.aria-current]="chat.selected()?.id === channel.id ? 'page' : null"
                      (click)="selectChannel(channel)">
                <b class="hash-mark">#</b><span>{{ channel.name }}</span>
                @if (channel.unreadCount) { <b class="nav-badge">{{ channel.unreadCount }}</b> }
              </button>
            }
          </section>

          <section class="nav-section" aria-labelledby="online-nav-title">
            <button class="nav-heading nav-heading-button" id="online-nav-title" type="button"
                    aria-label="Online people" title="Online people" (click)="showPeople()">
              <span>Online now</span><span>{{ presence.users().length }}</span>
            </button>
            @for (user of presence.users(); track user.userId) {
              <button class="nav-item person-nav-item" type="button"
                      [attr.aria-label]="user.displayName + ', online'" [attr.title]="user.displayName"
                      (click)="showPeople()">
                <i class="presence-dot" aria-hidden="true"></i><span>{{ user.displayName }}</span>
                <span class="sr-only">Online</span>
              </button>
            } @empty {
              <p class="nav-empty">Waiting for presence…</p>
            }
          </section>
        </nav>

        <div class="sidebar-profile">
          <span class="avatar avatar-indigo">{{ initials(auth.user()?.displayName || '') }}<i class="avatar-status"></i></span>
          <span class="profile-copy"><strong>{{ auth.user()?.displayName }}</strong><small>{{ auth.user()?.role }}</small></span>
          <button class="icon-button" type="button" aria-label="Sign out" title="Sign out" (click)="logout()">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 5H5v14h5M14 8l4 4-4 4M18 12H9"/></svg>
          </button>
        </div>
      </aside>

      <main class="conversation" [class.mobile-hidden]="mobilePane() !== 'chat'">
        <header class="conversation-header">
          <div class="channel-heading">
            <h1># {{ chat.selected()?.name || 'channel' }}</h1>
            <p>{{ chat.selected() ? 'Team conversation and project updates' : 'Choose a channel to start collaborating' }}</p>
          </div>
          @if (realtime.state$ | async; as state) {
            <span class="connection-pill" aria-live="polite"
                  [class.connected]="state === 'connected'"
                  [class.offline]="state === 'disconnected' || state === 'error'">
              <i></i>{{ connectionLabel(state) }}
            </span>
          }
        </header>

        <div class="message-stream" role="log" aria-live="polite"
             [attr.aria-label]="'Messages in ' + (chat.selected()?.name || 'selected channel')">
          <div class="stream-divider"><span>Recent messages</span></div>
          @for (message of chat.messages(); track message.id) {
            <article class="message-row" [class.own-message]="message.senderId === auth.user()?.id">
              <span class="avatar" [class.avatar-indigo]="message.senderRole === 'MANAGER'"
                    [class.avatar-mint]="message.senderRole === 'ADMIN'"
                    [class.avatar-cyan]="message.senderRole === 'USER'">
                {{ initials(message.senderName) }}
              </span>
              <div class="message-content">
                <header><strong>{{ message.senderName }}</strong><time [attr.datetime]="message.sentAt">{{ message.sentAt | date:'shortTime' }}</time></header>
                <p>{{ message.body }}</p>
              </div>
            </article>
          } @empty {
            <div class="workspace-empty">
              <span>#</span>
              <strong>{{ chat.selected() ? 'Start the conversation' : 'Choose a channel' }}</strong>
              <p>{{ chat.selected() ? 'Send the first message to this workspace.' : 'Your authorized channels appear in the navigation.' }}</p>
            </div>
          }
        </div>

        <form class="composer-region" (ngSubmit)="sendChat()">
          <div class="message-composer">
            <label class="sr-only" for="chat-message">Message {{ chat.selected()?.name || 'channel' }}</label>
            <textarea id="chat-message" name="message" [(ngModel)]="chatDraft" maxlength="2000"
                      [placeholder]="chat.selected() ? 'Message #' + chat.selected()?.name : 'Choose a channel to write a message'"
                      aria-label="Chat message" [disabled]="!chat.selected()"
                      (keydown.enter)="sendOnEnter($event)"></textarea>
            <div class="composer-footer">
              <span>Enter to send · Shift + Enter for a new line</span>
              <button class="send-button" type="submit" [disabled]="!chat.selected() || !chatDraft.trim()">
                Send
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m4 4 16 8-16 8 3-8zM7 12h13"/></svg>
              </button>
            </div>
          </div>
        </form>
      </main>

      @if (activityOpen()) {
        <aside class="activity-rail" [class.mobile-active]="mobilePane() !== 'chat'"
               [class.people-pane]="mobilePane() === 'people'" aria-labelledby="activity-title">
          <header class="activity-header">
            <div><h2 id="activity-title">{{ mobilePane() === 'people' ? 'People' : 'Activity' }}</h2>
              <p>{{ mobilePane() === 'people' ? presence.users().length + ' online now' : notifications.unreadCount() + ' unread updates' }}</p></div>
            <button class="icon-button" type="button" aria-label="Close activity panel" (click)="closeActivity()">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18"/></svg>
            </button>
          </header>

          <div class="activity-scroll">
            @if (canNotify()) {
              <section class="rail-section notification-compose-section" aria-labelledby="send-update-title">
                <div class="rail-heading"><h3 id="send-update-title">Send update</h3><span>Manager access</span></div>
                <form class="notification-composer" (ngSubmit)="createNotification()">
                  <label class="sr-only" for="notification-recipient">Notification recipient</label>
                  <select id="notification-recipient" name="recipient" [(ngModel)]="recipientId"
                          aria-label="Notification recipient" required>
                    <option [ngValue]="null">Choose teammate</option>
                    @for (user of users(); track user.id) {
                      <option [ngValue]="user.id">{{ user.displayName }} · {{ user.role }}</option>
                    }
                  </select>
                  <label class="sr-only" for="notification-message">Notification message</label>
                  <input id="notification-message" name="notificationMessage" [(ngModel)]="notificationMessage"
                         aria-label="Notification message" placeholder="Write a short update" maxlength="500" required>
                  <button type="submit" [disabled]="!recipientId || !notificationMessage.trim()">Send notification</button>
                  @if (notificationFeedback(); as feedback) {
                    <p class="inline-feedback" [class.error]="feedback.error" [attr.role]="feedback.error ? 'alert' : 'status'">{{ feedback.message }}</p>
                  }
                </form>
              </section>
            }

            <section class="rail-section notification-section" aria-labelledby="notifications-title">
              <div class="rail-heading"><h3 id="notifications-title">Notifications</h3><span>{{ notifications.items().length }} total</span></div>
              <div class="activity-list">
                @for (item of notifications.items(); track item.id) {
                  <button class="activity-item" [class.unread]="!item.readAt" type="button"
                          [attr.aria-label]="(!item.readAt ? 'Mark notification as read: ' : 'Notification: ') + (item.payload.message || item.type)"
                          (click)="notifications.markRead(item)">
                    <span class="notification-symbol" aria-hidden="true">{{ item.type.charAt(0).toUpperCase() }}</span>
                    <span><strong>{{ item.payload.message || item.type }}</strong><time [attr.datetime]="item.createdAt">{{ item.createdAt | date:'short' }}</time></span>
                    @if (!item.readAt) { <i class="unread-dot" aria-hidden="true"></i> }
                  </button>
                } @empty {
                  <div class="rail-empty"><strong>You're all caught up</strong><p>New notifications will appear here.</p></div>
                }
              </div>
            </section>

            <section class="rail-section people-section" aria-labelledby="people-title">
              <div class="rail-heading"><h3 id="people-title">Online now</h3><span>{{ presence.users().length }} people</span></div>
              <div class="people-list">
                @for (user of presence.users(); track user.userId) {
                  <div class="people-row">
                    <span class="avatar" [class.avatar-indigo]="user.role === 'MANAGER'"
                          [class.avatar-mint]="user.role === 'ADMIN'" [class.avatar-cyan]="user.role === 'USER'">
                      {{ initials(user.displayName) }}<i class="avatar-status"></i>
                    </span>
                    <span class="profile-copy"><strong>{{ user.displayName }}</strong><small>Active now</small></span>
                    <b class="role-tag">{{ user.role }}</b>
                  </div>
                } @empty {
                  <div class="rail-empty"><strong>No one else is online</strong><p>Presence updates appear here in real time.</p></div>
                }
              </div>
            </section>
          </div>
        </aside>
      }

      <nav class="mobile-navigation" aria-label="Mobile workspace navigation">
        <button type="button" [class.selected]="mobilePane() === 'chat'" [attr.aria-current]="mobilePane() === 'chat' ? 'page' : null" (click)="showChat()">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 5h16v12H7l-3 3z"/></svg><span>Chat</span>
        </button>
        <button type="button" [class.selected]="mobilePane() === 'activity'" [attr.aria-current]="mobilePane() === 'activity' ? 'page' : null" (click)="showActivity()">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/></svg><span>Activity</span>
          @if (notifications.unreadCount()) { <b>{{ notifications.unreadCount() }}</b> }
        </button>
        <button type="button" [class.selected]="mobilePane() === 'people'" [attr.aria-current]="mobilePane() === 'people' ? 'page' : null" (click)="showPeople()">
          <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="9" cy="8" r="3"/><circle cx="17" cy="10" r="2"/><path d="M3 20c0-4 2-6 6-6s6 2 6 6M15 15c4 0 6 2 6 5"/></svg><span>People</span>
        </button>
      </nav>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WorkspaceComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly realtime = inject(RealtimeService);
  readonly notifications = inject(NotificationStore);
  readonly chat = inject(ChatStore);
  readonly presence = inject(PresenceStore);
  private readonly api = inject(CollaborationApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly users = signal<UserSummary[]>([]);
  readonly error = signal('');
  readonly mobilePane = signal<WorkspacePane>('chat');
  readonly activityOpen = signal(true);
  readonly notificationFeedback = signal<{ message: string; error: boolean } | null>(null);
  recipientId: number | null = null;
  notificationMessage = '';
  chatDraft = '';

  constructor() {
    this.realtime.errors$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((message) => this.error.set(message));
  }

  ngOnInit(): void {
    this.api.users().subscribe({
      next: (users) => this.users.set(users),
      error: () => this.error.set('Team members could not be loaded.')
    });
    this.notifications.load();
    this.chat.loadChannels();
    this.realtime.connect();
  }

  canNotify(): boolean {
    return this.auth.hasRole('ADMIN') || this.auth.hasRole('MANAGER');
  }

  selectChannel(channel: Channel): void {
    this.chat.select(channel);
    this.showChat();
  }

  showChat(): void {
    this.mobilePane.set('chat');
  }

  showActivity(): void {
    this.mobilePane.set('activity');
    this.activityOpen.set(true);
  }

  showPeople(): void {
    this.mobilePane.set('people');
    this.activityOpen.set(true);
  }

  closeActivity(): void {
    this.activityOpen.set(false);
    this.mobilePane.set('chat');
  }

  connectionLabel(state: ConnectionState): string {
    return ({
      disconnected: 'Disconnected',
      connecting: 'Connecting',
      connected: 'Connected',
      reconnecting: 'Reconnecting',
      error: 'Connection error'
    })[state];
  }

  createNotification(): void {
    const message = this.notificationMessage.trim();
    if (!this.recipientId || !message) return;
    this.notificationFeedback.set(null);
    this.api.createNotification(this.recipientId, 'collaboration.notice', message).subscribe({
      next: () => {
        this.notificationMessage = '';
        this.notificationFeedback.set({ message: 'Notification sent.', error: false });
      },
      error: () => this.notificationFeedback.set({
        message: 'The notification could not be sent. Your message was kept.', error: true
      })
    });
  }

  sendOnEnter(event: Event): void {
    const keyboardEvent = event as KeyboardEvent;
    if (keyboardEvent.shiftKey) return;
    keyboardEvent.preventDefault();
    this.sendChat();
  }

  sendChat(): void {
    const body = this.chatDraft.trim();
    if (!body) return;
    try {
      this.chat.send(body);
      this.chatDraft = '';
    } catch {
      this.error.set('Chat is reconnecting. Try again in a moment.');
    }
  }

  initials(name: string): string {
    return name.split(/\s+/).filter(Boolean).slice(0, 2)
      .map((part) => part.charAt(0)).join('').toUpperCase();
  }

  logout(): void {
    void this.realtime.disconnect();
    this.auth.logout();
  }
}
