import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { SharedRoutingModule } from './shared-routing-module';
import { Navbar } from './components/navbar/navbar';
import { Footer } from './components/footer/footer';
import { About } from './components/about/about';
import { ToastComponent } from './components/toast/toast';
import { HttpClientModule } from '@angular/common/http';


@NgModule({
  declarations: [
    Navbar,
    Footer,
    About,
    ToastComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    SharedRoutingModule,
    HttpClientModule
  ],
  exports: [
    Navbar,
    Footer,
    ToastComponent
  ]
})
export class SharedModule { }
