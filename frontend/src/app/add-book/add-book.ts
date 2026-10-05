import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { BooksApi } from '../core/books-api';
import { ApiError, BookSummary } from '../core/models';

/**
 * "Add a new book" screen (US-11, extra). One textarea accepts a book's JSON three
 * ways — typed/pasted, picked via a hidden file input, or dragged-and-dropped as a
 * `.json` file — all feeding the same text content. The frontend only checks that the
 * text is syntactically valid JSON before calling `POST /api/books`; every actual
 * business rule is re-validated backend-side and rejection reasons are shown
 * verbatim, one per line (see `docs/05-business-architecture.md`).
 */
@Component({
  selector: 'app-add-book',
  styleUrl: './add-book.scss',
  templateUrl: './add-book.html',
})
export class AddBook {
  private readonly booksApi = inject(BooksApi);
  private readonly router = inject(Router);

  protected readonly jsonText = signal('');
  protected readonly submitting = signal(false);
  protected readonly syntaxError = signal<string | null>(null);
  protected readonly rejectionReasons = signal<string[] | null>(null);
  protected readonly addedBook = signal<BookSummary | null>(null);
  protected readonly dragOver = signal(false);

  protected onTextChange(value: string): void {
    this.jsonText.set(value);
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.readFile(file);
    }
    input.value = '';
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  protected onDragLeave(): void {
    this.dragOver.set(false);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.readFile(file);
    }
  }

  protected onSubmit(): void {
    this.syntaxError.set(null);
    this.rejectionReasons.set(null);

    const text = this.jsonText().trim();
    if (!text) {
      this.syntaxError.set('Paste or load a book\u2019s JSON before submitting.');
      return;
    }

    try {
      JSON.parse(text);
    } catch {
      this.syntaxError.set('That\u2019s not valid JSON \u2014 check for a missing comma, quote, or brace.');
      return;
    }

    this.submitting.set(true);
    this.booksApi.addBook(text).subscribe({
      next: (book) => {
        this.submitting.set(false);
        this.addedBook.set(book);
      },
      error: (response: HttpErrorResponse) => {
        this.submitting.set(false);
        const apiError = response.error as ApiError | undefined;
        this.rejectionReasons.set(
          apiError?.messages?.length ? apiError.messages : ['Something went wrong. Please try again.'],
        );
      },
    });
  }

  /** "Add another" — resets the form so a second book can be submitted right away. */
  protected onAddAnother(): void {
    this.jsonText.set('');
    this.syntaxError.set(null);
    this.rejectionReasons.set(null);
    this.addedBook.set(null);
  }

  protected onGoToLibrary(): void {
    this.router.navigate(['/']);
  }

  protected onBackClicked(event: Event): void {
    event.preventDefault();
    this.onGoToLibrary();
  }

  private readFile(file: File): void {
    const reader = new FileReader();
    reader.onload = () => {
      this.jsonText.set(String(reader.result ?? ''));
      this.syntaxError.set(null);
      this.rejectionReasons.set(null);
    };
    reader.readAsText(file);
  }
}
