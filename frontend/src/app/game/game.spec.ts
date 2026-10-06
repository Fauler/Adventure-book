import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';

import { Game } from './game';

const BOOK_DETAIL = {
  id: 'crystal-caverns',
  title: 'Crystal Caverns',
  author: 'A. Author',
  difficulty: 'EASY',
  sections: [
    { id: '1', text: 'You wake up in a forest.', options: [{ description: 'Go north' }] },
    { id: '2', text: 'You keep going.', options: [{ description: 'Run' }] },
  ],
};

describe('Game', () => {
  let component: Game;
  let fixture: ComponentFixture<Game>;
  let httpMock: HttpTestingController;

  const detailUrl = '/api/books/crystal-caverns';
  const progressUrl = '/api/books/crystal-caverns/progress';
  const playUrl = '/api/books/crystal-caverns/play';

  function flushDetail(detail: unknown = BOOK_DETAIL): void {
    httpMock.expectOne((r) => r.url === detailUrl).flush(detail as object);
  }

  function flushNoSave(): void {
    httpMock.expectOne((r) => r.url === progressUrl).flush(null, { status: 204, statusText: 'No Content' });
  }

  function flushSave(body: unknown): void {
    httpMock.expectOne((r) => r.url === progressUrl).flush(body as object);
  }

  function setUp(): void {
    TestBed.configureTestingModule({
      imports: [Game],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: 'crystal-caverns' }) } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Game);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => {
    httpMock.verify();
  });

  describe('fresh start (no saved progress)', () => {
    beforeEach(() => {
      setUp();
      flushDetail();
      flushNoSave();
    });

    it('should create and start a fresh game', () => {
      expect(component).toBeTruthy();
      const req = httpMock.expectOne((r) => r.url === playUrl);
      expect(req.request.body).toEqual({});
      req.flush({
        section: { id: '1', text: 'You wake up in a forest.', options: [{ description: 'Go north' }] },
        health: 10,
        status: 'PLAYING',
      });
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('You wake up in a forest.');
      expect(text).toContain('Go north');
      expect(text).toContain('10 HP');
      expect(text).toContain('Crystal Caverns');
    });

    it('posts the chosen optionIndex/currentSectionId/health and re-renders on the response', () => {
      httpMock.expectOne((r) => r.url === playUrl).flush({
        section: { id: '1', text: 'Section one', options: [{ description: 'Go north' }] },
        health: 10,
        status: 'PLAYING',
      });
      fixture.detectChanges();

      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.option')!.click();

      const req = httpMock.expectOne((r) => r.url === playUrl);
      expect(req.request.body).toEqual({ currentSectionId: '1', optionIndex: 0, health: 10 });
      req.flush({ section: { id: '2', text: 'The end.', options: [] }, health: 10, status: 'WON' });
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('You made it out alive!');
    });

    it('shows the consequence text and updated health after a move that triggers one (US-06)', () => {
      httpMock.expectOne((r) => r.url === playUrl).flush({
        section: { id: '1', text: 'Section one', options: [{ description: 'Touch the rusty nail' }] },
        health: 10,
        status: 'PLAYING',
        consequenceText: null,
      });
      fixture.detectChanges();

      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.option')!.click();

      const req = httpMock.expectOne((r) => r.url === playUrl);
      req.flush({
        section: { id: '2', text: 'You keep going.', options: [{ description: 'Run' }] },
        health: 7,
        status: 'PLAYING',
        consequenceText: 'You cut yourself on a rusty nail.',
      });
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('You cut yourself on a rusty nail.');
      expect(text).toContain('7 HP');
      expect((fixture.nativeElement as HTMLElement).querySelector('.health-delta')?.textContent?.trim()).toBe(
        '(-3 HP)',
      );
    });

    it('shows a "+" prefixed HP delta when the consequence is a gain', () => {
      httpMock.expectOne((r) => r.url === playUrl).flush({
        section: { id: '1', text: 'Section one', options: [{ description: 'Drink the potion' }] },
        health: 10,
        status: 'PLAYING',
        consequenceText: null,
      });
      fixture.detectChanges();

      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.option')!.click();

      const req = httpMock.expectOne((r) => r.url === playUrl);
      req.flush({
        section: { id: '2', text: 'You feel stronger.', options: [{ description: 'Continue' }] },
        health: 15,
        status: 'PLAYING',
        consequenceText: 'The potion restores your strength.',
      });
      fixture.detectChanges();

      const delta = (fixture.nativeElement as HTMLElement).querySelector('.health-delta');
      expect(delta?.textContent?.trim()).toBe('(+5 HP)');
      expect(delta?.classList.contains('gain')).toBe(true);
    });

    it('does not show an HP delta when a move has no consequence text', () => {
      httpMock.expectOne((r) => r.url === playUrl).flush({
        section: { id: '1', text: 'Section one', options: [{ description: 'Walk on' }] },
        health: 10,
        status: 'PLAYING',
        consequenceText: null,
      });
      fixture.detectChanges();

      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.option')!.click();

      const req = httpMock.expectOne((r) => r.url === playUrl);
      req.flush({
        section: { id: '2', text: 'Nothing happens.', options: [{ description: 'Continue' }] },
        health: 10,
        status: 'PLAYING',
        consequenceText: null,
      });
      fixture.detectChanges();

      expect((fixture.nativeElement as HTMLElement).querySelector('.health-delta')).toBeNull();
    });

    it('shows a distinct "You died" screen on DEAD status (US-07)', () => {
      httpMock.expectOne((r) => r.url === playUrl).flush({
        section: { id: '2', text: 'You bleed out.', options: [] },
        health: 0,
        status: 'DEAD',
        consequenceText: 'The poison finishes you off.',
      });
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('You died.');
      expect(text).toContain('The poison finishes you off.');
      expect(text).toContain('Back to Library');
      expect((fixture.nativeElement as HTMLElement).querySelector('button.option')).toBeNull();
    });

    it('leaves immediately on Stop once progress has been explicitly saved and nothing changed since', () => {
      httpMock.expectOne((r) => r.url === playUrl).flush({
        section: { id: '1', text: 'Section one', options: [{ description: 'Go north' }] },
        health: 10,
        status: 'PLAYING',
        consequenceText: null,
      });
      fixture.detectChanges();

      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.save')!.click();
      httpMock
        .expectOne((r) => r.url === progressUrl && r.method === 'PUT')
        .flush({ currentSectionId: '1', health: 10, updatedAt: '2026-01-01T00:00:00Z' });
      fixture.detectChanges();

      const router = TestBed.inject(Router);
      const navigateSpy = vi.spyOn(router, 'navigateByUrl');

      const stopButton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.stop');
      stopButton!.click();
      fixture.detectChanges();

      expect(navigateSpy).toHaveBeenCalledWith('/');
      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).not.toContain('unsaved progress');
    });

    it('the header "Back to Library" link also prompts to save when there is unsaved progress (no bypass)', () => {
      httpMock.expectOne((r) => r.url === playUrl).flush({
        section: { id: '1', text: 'Section one', options: [{ description: 'Go north' }] },
        health: 10,
        status: 'PLAYING',
        consequenceText: null,
      });
      fixture.detectChanges();

      const router = TestBed.inject(Router);
      const navigateSpy = vi.spyOn(router, 'navigateByUrl');

      const backLink = (fixture.nativeElement as HTMLElement).querySelector<HTMLAnchorElement>('a.back');
      backLink!.click();
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('unsaved progress');
      expect(navigateSpy).not.toHaveBeenCalled();
    });

    it('prompts to save on Stop when there is unsaved progress, and leaves on "Leave without saving"', () => {
      httpMock.expectOne((r) => r.url === playUrl).flush({
        section: { id: '1', text: 'Section one', options: [{ description: 'Go north' }] },
        health: 10,
        status: 'PLAYING',
        consequenceText: null,
      });
      fixture.detectChanges();

      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.option')!.click();
      httpMock.expectOne((r) => r.url === playUrl).flush({
        section: { id: '2', text: 'You keep going.', options: [{ description: 'Run' }] },
        health: 7,
        status: 'PLAYING',
        consequenceText: null,
      });
      fixture.detectChanges();

      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.stop')!.click();
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('unsaved progress');

      const router = TestBed.inject(Router);
      const navigateSpy = vi.spyOn(router, 'navigateByUrl');

      const buttons = Array.from(
        (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.modal-actions button'),
      );
      const leaveButton = buttons.find((b) => b.textContent?.includes('Leave without saving'));
      leaveButton!.click();
      fixture.detectChanges();

      expect(navigateSpy).toHaveBeenCalledWith('/');
    });

    it('cancelling the stop-confirmation keeps the player on the current section', () => {
      httpMock.expectOne((r) => r.url === playUrl).flush({
        section: { id: '1', text: 'Section one', options: [{ description: 'Go north' }] },
        health: 10,
        status: 'PLAYING',
        consequenceText: null,
      });
      fixture.detectChanges();

      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.stop')!.click();
      fixture.detectChanges();

      const buttons = Array.from(
        (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.modal-actions button'),
      );
      const cancelButton = buttons.find((b) => b.textContent?.includes('Cancel'));
      cancelButton!.click();
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).not.toContain('unsaved progress');
      expect(text).toContain('Section one');
    });

    it('Save & Leave from the stop-confirmation saves progress then navigates away', () => {
      httpMock.expectOne((r) => r.url === playUrl).flush({
        section: { id: '1', text: 'Section one', options: [{ description: 'Go north' }] },
        health: 10,
        status: 'PLAYING',
        consequenceText: null,
      });
      fixture.detectChanges();

      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.stop')!.click();
      fixture.detectChanges();

      const router = TestBed.inject(Router);
      const navigateSpy = vi.spyOn(router, 'navigateByUrl');

      const buttons = Array.from(
        (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.modal-actions button'),
      );
      const saveAndLeaveButton = buttons.find((b) => b.textContent?.includes('Save'));
      saveAndLeaveButton!.click();

      const req = httpMock.expectOne((r) => r.url === progressUrl && r.method === 'PUT');
      expect(req.request.body).toEqual({ currentSectionId: '1', health: 10 });
      req.flush({ currentSectionId: '1', health: 10, updatedAt: '2026-01-01T00:00:00Z' });

      expect(navigateSpy).toHaveBeenCalledWith('/');
    });

    it('saves progress via the Save Progress button and shows feedback', () => {
      httpMock.expectOne((r) => r.url === playUrl).flush({
        section: { id: '1', text: 'Section one', options: [{ description: 'Go north' }] },
        health: 10,
        status: 'PLAYING',
        consequenceText: null,
      });
      fixture.detectChanges();

      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.save')!.click();

      const req = httpMock.expectOne((r) => r.url === progressUrl && r.method === 'PUT');
      expect(req.request.body).toEqual({ currentSectionId: '1', health: 10 });
      req.flush({ currentSectionId: '1', health: 10, updatedAt: '2026-01-01T00:00:00Z' });
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('Saved!');
    });

    it('shows an error message when saving fails', () => {
      httpMock.expectOne((r) => r.url === playUrl).flush({
        section: { id: '1', text: 'Section one', options: [{ description: 'Go north' }] },
        health: 10,
        status: 'PLAYING',
        consequenceText: null,
      });
      fixture.detectChanges();

      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.save')!.click();

      httpMock
        .expectOne((r) => r.url === progressUrl && r.method === 'PUT')
        .flush('boom', { status: 500, statusText: 'Internal Server Error' });
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('Could not save');
    });

    it('does not show a Stop/Save control on the ending screen (US-08)', () => {
      httpMock.expectOne((r) => r.url === playUrl).flush({
        section: { id: '1', text: 'Section one', options: [{ description: 'Go north' }] },
        health: 10,
        status: 'PLAYING',
        consequenceText: null,
      });
      fixture.detectChanges();

      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.option')!.click();
      httpMock
        .expectOne((r) => r.url === playUrl)
        .flush({ section: { id: '2', text: 'The end.', options: [] }, health: 10, status: 'WON', consequenceText: null });
      fixture.detectChanges();

      expect((fixture.nativeElement as HTMLElement).querySelector('button.stop')).toBeNull();
      expect((fixture.nativeElement as HTMLElement).querySelector('button.save')).toBeNull();
    });
  });

  describe('resuming saved progress (US-10)', () => {
    it('shows the resume modal and Continue renders the saved section/health without calling /play', () => {
      setUp();
      flushDetail();
      flushSave({ currentSectionId: '2', health: 7, updatedAt: '2026-01-01T00:00:00Z' });
      fixture.detectChanges();

      let text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('Resume your adventure?');

      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.primary')!.click();
      fixture.detectChanges();

      text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('You keep going.');
      expect(text).toContain('7 HP');
      httpMock.expectNone(playUrl);
    });

    it('Restart deletes the save and then starts a fresh game', () => {
      setUp();
      flushDetail();
      flushSave({ currentSectionId: '2', health: 7, updatedAt: '2026-01-01T00:00:00Z' });
      fixture.detectChanges();

      const buttons = Array.from(
        (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.modal-actions button'),
      );
      const restartButton = buttons.find((b) => b.textContent?.includes('Restart'));
      restartButton!.click();

      httpMock.expectOne((r) => r.url === progressUrl && r.method === 'DELETE').flush(null);
      const req = httpMock.expectOne((r) => r.url === playUrl);
      req.flush({
        section: { id: '1', text: 'You wake up in a forest.', options: [{ description: 'Go north' }] },
        health: 10,
        status: 'PLAYING',
      });
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('You wake up in a forest.');
    });

    it('shows a clear error and only a Restart option when the saved section no longer exists in the book', () => {
      setUp();
      flushDetail();
      flushSave({ currentSectionId: 'ghost-section', health: 5, updatedAt: '2026-01-01T00:00:00Z' });
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('no longer exists');
      expect((fixture.nativeElement as HTMLElement).querySelectorAll('.modal-actions button').length).toBe(1);
    });
  });

  describe('error handling', () => {
    it('shows an error state when the book detail fetch fails', () => {
      setUp();
      httpMock.expectOne((r) => r.url === detailUrl).flush('boom', { status: 500, statusText: 'Internal Server Error' });
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('Something went wrong');
    });

    it('falls back to starting a fresh game when the save-check fails', () => {
      setUp();
      flushDetail();
      httpMock
        .expectOne((r) => r.url === progressUrl)
        .flush('boom', { status: 500, statusText: 'Internal Server Error' });

      const req = httpMock.expectOne((r) => r.url === playUrl);
      req.flush({
        section: { id: '1', text: 'You wake up in a forest.', options: [{ description: 'Go north' }] },
        health: 10,
        status: 'PLAYING',
      });
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('You wake up in a forest.');
    });

    it('shows an error state when the /play call fails', () => {
      setUp();
      flushDetail();
      flushNoSave();
      httpMock.expectOne((r) => r.url === playUrl).flush('boom', {
        status: 500,
        statusText: 'Internal Server Error',
      });
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('Something went wrong');
    });
  });
});
