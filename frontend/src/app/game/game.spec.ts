import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { Game } from './game';

describe('Game', () => {
  let component: Game;
  let fixture: ComponentFixture<Game>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
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
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    httpMock.expectOne((r) => r.url === '/api/books/crystal-caverns/play').flush({
      section: { id: '1', text: 'begin', options: [] },
      health: 10,
      status: 'PLAYING',
    });
  });

  it('starts a fresh game on init and renders the BEGIN section', () => {
    const req = httpMock.expectOne((r) => r.url === '/api/books/crystal-caverns/play');
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
    expect(text).toContain('Health: 10');
  });

  it('posts the chosen optionIndex/currentSectionId/health and re-renders on the response', () => {
    httpMock.expectOne((r) => r.url === '/api/books/crystal-caverns/play').flush({
      section: { id: '1', text: 'Section one', options: [{ description: 'Go north' }] },
      health: 10,
      status: 'PLAYING',
    });
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.option')!.click();

    const req = httpMock.expectOne((r) => r.url === '/api/books/crystal-caverns/play');
    expect(req.request.body).toEqual({ currentSectionId: '1', optionIndex: 0, health: 10 });
    req.flush({ section: { id: '2', text: 'The end.', options: [] }, health: 10, status: 'WON' });
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('You won!');
  });

  it('shows an error state when the backend call fails', () => {
    httpMock.expectOne((r) => r.url === '/api/books/crystal-caverns/play').flush('boom', {
      status: 500,
      statusText: 'Internal Server Error',
    });
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Something went wrong');
  });
});

