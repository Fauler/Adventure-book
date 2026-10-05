import { Routes } from '@angular/router';

import { Game } from './game/game';
import { Home } from './home/home';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'books/:id/play', component: Game },
  { path: '**', redirectTo: '' },
];
