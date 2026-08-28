import { AppEnvironment } from './environment.model';

const webSocketProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';

export const environment: AppEnvironment = {
  production: true,
  apiUrl: '/api',
  webSocketUrl: `${webSocketProtocol}//${window.location.host}/ws`
};
