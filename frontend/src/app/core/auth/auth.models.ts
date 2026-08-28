export type Role = 'ADMIN' | 'MANAGER' | 'USER';

export interface CurrentUser {
  id: number;
  email: string;
  displayName: string;
  role: Role;
}

export interface AuthResponse extends CurrentUser {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

