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
    expect(text).toContain('1 Adventure Available');
  });

  it('shows a "no results" state when the backend returns an empty list', () => {
    const req = httpMock.expectOne((r) => r.url === '/api/books');
    req.flush([]);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('No adventures match your search.');
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
});
