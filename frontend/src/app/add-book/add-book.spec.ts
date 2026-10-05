import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { AddBook } from './add-book';

describe('AddBook', () => {
  let component: AddBook;
  let fixture: ComponentFixture<AddBook>;
  let httpMock: HttpTestingController;
  let router: Router;

  const VALID_JSON = '{"title":"A Tiny Tale","author":"Tester","difficulty":"EASY","sections":[]}';

  const textarea = () => (fixture.nativeElement as HTMLElement).querySelector('textarea.json-input') as HTMLTextAreaElement;
  const submitButton = () => (fixture.nativeElement as HTMLElement).querySelector('.actions .primary') as HTMLButtonElement;
  const statusText = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddBook],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(AddBook);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows a friendly error and does not call the backend when the text is not valid JSON', () => {
    textarea().value = '{not valid json';
    textarea().dispatchEvent(new Event('input'));
    fixture.detectChanges();

    submitButton().click();
    fixture.detectChanges();

    expect(statusText()).toContain('not valid JSON');
    httpMock.expectNone('/api/books');
  });

  it('requires some text before submitting', () => {
    submitButton().click();
    fixture.detectChanges();

    expect(statusText()).toContain('Paste or load');
    httpMock.expectNone('/api/books');
  });

  it('submits syntactically valid JSON to POST /api/books and shows the success panel', () => {
    textarea().value = VALID_JSON;
    textarea().dispatchEvent(new Event('input'));
    fixture.detectChanges();

    submitButton().click();

    const req = httpMock.expectOne((r) => r.url === '/api/books' && r.method === 'POST');
    expect(req.request.body).toBe(VALID_JSON);
    req.flush({ id: 'a_tiny_tale', title: 'A Tiny Tale', author: 'Tester', difficulty: 'EASY' });
    fixture.detectChanges();

    expect(statusText()).toContain('Book added');
    expect(statusText()).toContain('A Tiny Tale');
  });

  it('displays every rejection reason verbatim, one per line, on a 400', () => {
    textarea().value = VALID_JSON;
    textarea().dispatchEvent(new Event('input'));
    fixture.detectChanges();

    submitButton().click();

    const req = httpMock.expectOne((r) => r.url === '/api/books' && r.method === 'POST');
    req.flush(
      { timestamp: new Date().toISOString(), status: 400, error: 'Bad Request', messages: ['no BEGIN section found', 'no END section found'] },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();

    expect(statusText()).toContain('no BEGIN section found');
    expect(statusText()).toContain('no END section found');
  });

  it('"Add another" resets the form back to the input view', () => {
    textarea().value = VALID_JSON;
    textarea().dispatchEvent(new Event('input'));
    fixture.detectChanges();

    submitButton().click();
    const req = httpMock.expectOne((r) => r.url === '/api/books' && r.method === 'POST');
    req.flush({ id: 'a_tiny_tale', title: 'A Tiny Tale', author: 'Tester', difficulty: 'EASY' });
    fixture.detectChanges();

    const addAnother = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).find(
      (btn) => btn.textContent?.includes('Add another'),
    ) as HTMLButtonElement;
    addAnother.click();
    fixture.detectChanges();

    expect(statusText()).not.toContain('Book added');
    expect(textarea().value).toBe('');
  });

  it('"Go to Library" navigates home', () => {
    textarea().value = VALID_JSON;
    textarea().dispatchEvent(new Event('input'));
    fixture.detectChanges();

    submitButton().click();
    const req = httpMock.expectOne((r) => r.url === '/api/books' && r.method === 'POST');
    req.flush({ id: 'a_tiny_tale', title: 'A Tiny Tale', author: 'Tester', difficulty: 'EASY' });
    fixture.detectChanges();

    const navigateSpy = vi.spyOn(router, 'navigate');
    const goToLibrary = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).find(
      (btn) => btn.textContent?.includes('Go to Library'),
    ) as HTMLButtonElement;
    goToLibrary.click();

    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });
});
