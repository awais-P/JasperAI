import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { DashboardRoutingModule } from './dashboard-routing-module';
import { DashboardPage } from './dashboard-page/dashboard-page';


@NgModule({
  declarations: [
    DashboardPage
  ],
  imports: [
    CommonModule,
    FormsModule,
    DashboardRoutingModule
  ]
})
export class DashboardModule { }
