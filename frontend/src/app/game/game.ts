import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { GameApi } from '../core/game-api';
import { PlayResponse } from '../core/models';

/**
 * Play screen (Objective 2/US-05): shows the current section's text and options,
 * posts the chosen option to `/play`, and re-renders with the response. Backend is
 * the single source of truth — this component never computes the next section or
 * health itself, it only displays whatever `/play` returns.
 */
@Component({
  imports: [RouterLink],
  selector: 'app-game',
  styleUrl: './game.scss',
  templateUrl: './game.html',
})
export class Game implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly gameApi = inject(GameApi);

  private bookId = '';

  protected readonly state = signal<PlayResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal(false);

  ngOnInit(): void {
    this.bookId = this.route.snapshot.paramMap.get('id') ?? '';
    this.startGame();
  }

  protected startGame(): void {
    this.loading.set(true);
    this.error.set(false);
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
        },
        error: () => {
          this.error.set(true);
          this.loading.set(false);
        },
      });
  }
}
