import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Home } from './home';

describe('Home', () => {
  let component: Home;
  let fixture: ComponentFixture<Home>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Home);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    httpMock.expectOne((r) => r.url === '/api/books').flush([]);
  });

  it('fetches books from GET /api/books on init and renders the count', () => {
    const req = httpMock.expectOne((r) => r.url === '/api/books');
    req.flush([
      { id: 'crystal-caverns', title: 'The Crystal Caverns', author: 'Evelyn Stormrider', difficulty: 'EASY' },
    ]);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('The Crystal Caverns');
    expect(text).toContain('1 Epic Adventure Available');
  });

  it('shows a "no results" state when the backend returns an empty list', () => {
    const req = httpMock.expectOne((r) => r.url === '/api/books');
    req.flush([]);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('No adventures match your search.');
  });

  it('shows a genre pill only when the book has a non-blank type', () => {
    const req = httpMock.expectOne((r) => r.url === '/api/books');
    req.flush([
      {
        id: 'crystal-caverns',
        title: 'The Crystal Caverns',
        author: 'Evelyn Stormrider',
        difficulty: 'EASY',
        type: 'Fantasy',
      },
      { id: 'pirates-jade-sea', title: 'Pirates of the Jade Sea', author: 'Marina Blackwood', difficulty: 'MEDIUM' },
    ]);
    fixture.detectChanges();

    const genrePills = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.genre')).map((el) =>
      el.textContent?.trim(),
    );
    expect(genrePills).toEqual(['Fantasy']);
  });

  it('shows description, duration, chapter count and tags only when provided', () => {
    const req = httpMock.expectOne((r) => r.url === '/api/books');
    req.flush([
      {
        id: 'crystal-caverns',
        title: 'The Crystal Caverns',
        author: 'Evelyn Stormrider',
        difficulty: 'EASY',
        description: 'Deep beneath the mountain lies a hidden kingdom.',
        estimatedDuration: '45-60 min',
        chapterCount: 12,
        tags: ['Magic', 'Underground', 'Crystals'],
      },
      { id: 'pirates-jade-sea', title: 'Pirates of the Jade Sea', author: 'Marina Blackwood', difficulty: 'MEDIUM' },
    ]);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    const text = el.textContent ?? '';
    expect(text).toContain('Deep beneath the mountain lies a hidden kingdom.');
    expect(text).toContain('45-60 min');
    expect(text).toContain('12 chapters');

    const tagPills = Array.from(el.querySelectorAll('.tag')).map((e) => e.textContent?.trim());
    expect(tagPills).toEqual(['Magic', 'Underground', 'Crystals']);

    expect(el.querySelectorAll('.description').length).toBe(1);
    expect(el.querySelectorAll('.meta').length).toBe(1);
    expect(el.querySelectorAll('.tags').length).toBe(1);
  });

  it('re-fetches with the selected difficulty when a filter pill is clicked', () => {
    httpMock.expectOne((r) => r.url === '/api/books').flush([]);
    fixture.detectChanges();

    const easyPill = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button.pill')).find(
      (btn) => btn.textContent?.trim() === 'Easy',
    ) as HTMLButtonElement;
    easyPill.click();

    const req = httpMock.expectOne((r) => r.url === '/api/books' && r.params.get('difficulty') === 'EASY');
    req.flush([]);
  });

  it('renders an "+ Add a new book" entry point linking to /books/new', () => {
    httpMock.expectOne((r) => r.url === '/api/books').flush([]);
    fixture.detectChanges();

    const link = (fixture.nativeElement as HTMLElement).querySelector('a.add-book-link') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/books/new');
    expect(link.textContent).toContain('Add a new book');
  });
});
