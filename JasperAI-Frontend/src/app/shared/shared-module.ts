import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { SharedRoutingModule } from './shared-routing-module';
import { Navbar } from './components/navbar/navbar';
import { Footer } from './components/footer/footer';
import { About } from './components/about/about';


@NgModule({
  declarations: [
    Navbar,
    Footer,
    About
  ],
  imports: [
    CommonModule,
    RouterModule,
    SharedRoutingModule
  ],
  exports: [
    Navbar,
    Footer
  ]
})
export class SharedModule { }
