import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./components/home/home-module').then(m => m.HomeModule)
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./components/dashboard/dashboard-module').then(m => m.DashboardModule)
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
