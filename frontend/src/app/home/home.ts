import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { BooksApi } from '../core/books-api';
import { BookSummary, Difficulty } from '../core/models';

const DIFFICULTY_FILTERS: Array<{ label: string; value: Difficulty | null }> = [
  { label: 'All', value: null },
  { label: 'Easy', value: 'EASY' },
  { label: 'Medium', value: 'MEDIUM' },
  { label: 'Hard', value: 'HARD' },
];

/**
 * Home page: hero banner (US-02) + library grid with search/difficulty filter
 * (US-03) + "Begin Quest" entry point into the game (US-04). All filtering is
 * delegated to the backend (`GET /api/books?search=&difficulty=`) — this
 * component only renders what it gets back, never re-filters locally.
 */
@Component({
  imports: [RouterLink],
  selector: 'app-home',
  styleUrl: './home.scss',
  templateUrl: './home.html',
})
export class Home implements OnInit {
  private readonly booksApi = inject(BooksApi);
  private searchDebounce?: ReturnType<typeof setTimeout>;

  protected readonly difficultyFilters = DIFFICULTY_FILTERS;

  protected readonly books = signal<BookSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly searchTerm = signal('');
  protected readonly selectedDifficulty = signal<Difficulty | null>(null);

  ngOnInit(): void {
    this.fetchBooks();
  }

  protected onSearchInput(value: string): void {
    this.searchTerm.set(value);
    clearTimeout(this.searchDebounce);
    this.searchDebounce = setTimeout(() => this.fetchBooks(), 300);
  }

  protected onDifficultySelect(value: Difficulty | null): void {
    this.selectedDifficulty.set(value);
    this.fetchBooks();
  }

  private fetchBooks(): void {
    this.loading.set(true);
    this.error.set(false);
    this.booksApi.list(this.searchTerm(), this.selectedDifficulty()).subscribe({
      next: (books) => {
        this.books.set(books);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
