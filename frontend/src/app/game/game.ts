import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { BooksApi } from '../core/books-api';
import { GameApi } from '../core/game-api';
import { BookDetail, PlayResponse, SavedProgress } from '../core/models';

/**
 * Play screen (Objective 2/US-05, extended in M4 for US-08/09/10 save & resume).
 * Backend is the single source of truth — this component never computes the next
 * section or health itself, it only displays whatever `/play` returns, or (for
 * **Continue**) looks up the saved section's own content from the already-fetched
 * `BookDetail` rather than inventing it.
 */
@Component({
  imports: [RouterLink],
  selector: 'app-game',
  styleUrl: './game.scss',
  templateUrl: './game.html',
})
export class Game implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly gameApi = inject(GameApi);
  private readonly booksApi = inject(BooksApi);

  private bookId = '';
  private bookDetail: BookDetail | null = null;

  protected readonly state = signal<PlayResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly bookTitle = signal<string | null>(null);

  /** Set once a save is detected on init; drives the "Resume your adventure?" modal
   * (US-10) — shown *before* any section is rendered. */
  protected readonly pendingResume = signal<SavedProgress | null>(null);
  /** True if {@link pendingResume}'s saved section no longer exists in the book's
   * current detail (US-10's "book changed/became invalid" AC) — shown as a clear error
   * inside the resume modal instead of silently restarting or crashing. */
  protected readonly resumeBroken = signal(false);

  /** The section id/health last confirmed saved on the backend, or `null` if nothing
   * has been saved yet this session — used to detect "unsaved progress" for US-08's
   * revisit (Stop should prompt to save first when this differs from {@link state}). */
  private lastSaved: { sectionId: string; health: number } | null = null;
  protected readonly saveState = signal<'idle' | 'saving' | 'saved' | 'error'>('idle');
  /** Shows the "unsaved progress" confirmation instead of immediately leaving. */
  protected readonly confirmStop = signal(false);

  ngOnInit(): void {
    this.bookId = this.route.snapshot.paramMap.get('id') ?? '';
    this.loading.set(true);
    this.error.set(false);

    // Per `docs/05-business-architecture.md`: on opening the game screen, the book
    // detail is fetched and, in parallel, the backend is asked whether a save exists.
    this.booksApi.getDetail(this.bookId).subscribe({
      next: (detail) => {
        this.bookDetail = detail;
        this.bookTitle.set(detail.title);
        this.booksApi.getProgress(this.bookId).subscribe({
          next: (progress) => {
            this.loading.set(false);
            if (progress) {
              this.pendingResume.set(progress);
              this.resumeBroken.set(!detail.sections.some((s) => s.id === progress.currentSectionId));
            } else {
              this.startGame();
            }
          },
          error: () => {
            // A failed save-check must not block play entirely — fall back to a
            // fresh game, same as "no save exists".
            this.loading.set(false);
            this.startGame();
          },
        });
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  /** **Continue** (US-10): jump straight to the saved section/health, no `/play` call. */
  protected resumeContinue(): void {
    const saved = this.pendingResume();
    const section = this.bookDetail?.sections.find((s) => s.id === saved?.currentSectionId);
    if (!saved || !section) {
      return;
    }
    this.pendingResume.set(null);
    this.state.set({ section, health: saved.health, status: 'PLAYING', consequenceText: null });
    this.lastSaved = { sectionId: saved.currentSectionId, health: saved.health };
  }

  /** **Restart** (US-10): clears the save immediately, then begins fresh at BEGIN. */
  protected resumeRestart(): void {
    this.pendingResume.set(null);
    this.resumeBroken.set(false);
    this.loading.set(true);
    this.booksApi.deleteProgress(this.bookId).subscribe({
      next: () => this.startGame(),
      error: () => this.startGame(),
    });
  }

  protected startGame(): void {
    this.loading.set(true);
    this.error.set(false);
    this.lastSaved = null;
    this.gameApi.play(this.bookId, {}).subscribe({
      next: (response) => {
        this.state.set(response);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  protected chooseOption(optionIndex: number): void {
    const current = this.state();
    if (!current) {
      return;
    }
    this.loading.set(true);
    this.error.set(false);
    this.gameApi
      .play(this.bookId, {
        currentSectionId: current.section.id,
        optionIndex,
        health: current.health,
      })
      .subscribe({
        next: (response) => {
          this.state.set(response);
          this.loading.set(false);
          if (response.status !== 'PLAYING') {
            // Auto-cleared server-side too; keeps the frontend's own tracking in sync.
            this.lastSaved = null;
          }
        },
        error: () => {
          this.error.set(true);
          this.loading.set(false);
        },
      });
  }

  /** "Save Progress" header button (US-09). */
  protected saveProgress(): void {
    const current = this.state();
    if (!current || current.status !== 'PLAYING') {
      return;
    }
    this.saveState.set('saving');
    this.booksApi
      .saveProgress(this.bookId, { currentSectionId: current.section.id, health: current.health })
      .subscribe({
        next: () => {
          this.lastSaved = { sectionId: current.section.id, health: current.health };
          this.saveState.set('saved');
          setTimeout(() => this.saveState.set('idle'), 2000);
        },
        error: () => this.saveState.set('error'),
      });
  }

  protected hasUnsavedProgress(): boolean {
    const current = this.state();
    if (!current || current.status !== 'PLAYING') {
      return false;
    }
    return (
      this.lastSaved === null ||
      this.lastSaved.sectionId !== current.section.id ||
      this.lastSaved.health !== current.health
    );
  }

  /** US-08 revisit: prompt to save first when there's unsaved progress. */
  protected onStopClicked(): void {
    if (this.hasUnsavedProgress()) {
      this.confirmStop.set(true);
    } else {
      this.leaveLibrary();
    }
  }

  /** The header's "← Back to Library" link is a second exit point alongside Stop — it
   * must go through the same unsaved-progress check rather than navigating directly,
   * otherwise a player could bypass the save prompt entirely. */
  protected onBackClicked(event: Event): void {
    event.preventDefault();
    this.onStopClicked();
  }

  protected saveAndStop(): void {
    const current = this.state();
    if (!current) {
      this.leaveLibrary();
      return;
    }
    this.booksApi
      .saveProgress(this.bookId, { currentSectionId: current.section.id, health: current.health })
      .subscribe({
        next: () => this.leaveLibrary(),
        error: () => this.leaveLibrary(), // don't trap the player if save fails on exit
      });
  }

  protected leaveWithoutSaving(): void {
    this.leaveLibrary();
  }

  protected cancelStop(): void {
    this.confirmStop.set(false);
  }

  private leaveLibrary(): void {
    this.router.navigateByUrl('/');
  }
}
