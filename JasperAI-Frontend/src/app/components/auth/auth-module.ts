import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { AuthRoutingModule } from './auth-routing-module';
import { Login } from './login/login';
import { Signup } from './signup/signup';


@NgModule({
  declarations: [
    Login,
    Signup
  ],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    AuthRoutingModule
  ]
})
export class AuthModule { }
