import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { LoginComponent } from './features/login/login.component';
import { WorkspaceComponent } from './features/workspace/workspace.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', component: WorkspaceComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];

