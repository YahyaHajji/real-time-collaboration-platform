import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Channel,
  ChatMessage,
  NotificationItem,
  NotificationPage,
  PresenceUser,
  UserSummary
} from '../models/collaboration.models';

@Injectable({ providedIn: 'root' })
export class CollaborationApiService {
  private readonly http = inject(HttpClient);

  notifications(after?: string): Observable<NotificationPage> {
    const params = after ? new HttpParams().set('after', after) : undefined;
    return this.http.get<NotificationPage>(`${environment.apiUrl}/notifications`, { params });
  }

  markNotificationRead(id: string): Observable<NotificationItem> {
    return this.http.patch<NotificationItem>(`${environment.apiUrl}/notifications/${id}/read`, {});
  }

  createNotification(recipientUserId: number, type: string, message: string): Observable<NotificationItem> {
    return this.http.post<NotificationItem>(`${environment.apiUrl}/notifications`, {
      recipientUserId,
      type,
      message
    });
  }

  users(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(`${environment.apiUrl}/users`);
  }

  channels(): Observable<Channel[]> {
    return this.http.get<Channel[]>(`${environment.apiUrl}/channels`);
  }

  history(channelId: string, before?: string): Observable<ChatMessage[]> {
    const params = before ? new HttpParams().set('before', before) : undefined;
    return this.http.get<ChatMessage[]>(`${environment.apiUrl}/channels/${channelId}/messages`, { params });
  }

  markChannelRead(channelId: string, lastReadMessageId: string): Observable<void> {
    return this.http.put<void>(`${environment.apiUrl}/channels/${channelId}/read-cursor`, {
      lastReadMessageId
    });
  }

  presence(): Observable<PresenceUser[]> {
    return this.http.get<PresenceUser[]>(`${environment.apiUrl}/presence`);
  }
}

