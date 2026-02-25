import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './shared/guards/auth-gaurd-guard';

const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./components/home/home-module').then(m => m.HomeModule)
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./components/dashboard/dashboard-module').then(m => m.DashboardModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'auth',
    loadChildren: () => import('./components/auth/auth-module').then(m => m.AuthModule)
  },
  {
    path: 'about',
    loadChildren: () => import('./shared/shared-module').then(m => m.SharedModule)
  },
  {
    path: 'profile',
    loadChildren: () => import('./components/profile/profile-module').then(m => m.ProfileModule),
    canActivate: [AuthGuard]
  },
  {
    path: '**',
    redirectTo: '',
    pathMatch: 'full'
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
