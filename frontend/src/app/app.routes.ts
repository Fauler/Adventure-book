import { Routes } from '@angular/router';

import { AddBook } from './add-book/add-book';
import { Game } from './game/game';
import { Home } from './home/home';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'books/new', component: AddBook },
  { path: 'books/:id/play', component: Game },
  { path: '**', redirectTo: '' },
];
