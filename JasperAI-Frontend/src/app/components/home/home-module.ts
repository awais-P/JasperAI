import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { HomeRoutingModule } from './home-routing-module';
import { HomePage } from './home-page/home-page';


@NgModule({
  declarations: [
    HomePage
  ],
  imports: [
    CommonModule,
    RouterModule,
    HomeRoutingModule
  ]
})
export class HomeModule { }
