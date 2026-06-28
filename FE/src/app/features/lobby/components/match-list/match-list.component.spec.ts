import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MatchListComponent } from './match-list.component';
import { MatchResponse } from '../../../../core/api/match-api.service';

describe('MatchListComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatchListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load matches on init', () => {
    const fixture = TestBed.createComponent(MatchListComponent);
    const component = fixture.componentInstance;

    fixture.detectChanges();

    const req = httpMock.expectOne('http://localhost:8080/api/matches');
    const dummyMatches: MatchResponse[] = [
      {
        id: 'm1',
        status: 'WAITING',
        currentPhase: null,
        turnNumber: 0,
        currentPlayerId: null,
        firstPlayerId: null,
        winnerPlayerId: null,
        finishReason: null,
        players: [{ playerId: 'p1', side: 'PLAYER_ONE', displayName: 'Player1' }],
        createdAt: '2026-06-08T00:00:00Z',
      },
    ];
    req.flush(dummyMatches);

    expect(component.matches().length).toBe(1);
    expect(component.matches()[0].id).toBe('m1');
    expect(component.matches()[0].hostName).toBe('Player1');
  });

  it('should show empty list on API error', () => {
    const fixture = TestBed.createComponent(MatchListComponent);
    const component = fixture.componentInstance;

    fixture.detectChanges();

    const req = httpMock.expectOne('http://localhost:8080/api/matches');
    req.error(new ProgressEvent('Network error'));

    expect(component.matches().length).toBe(0);
  });

  it('should refresh matches on onRefresh call', () => {
    const fixture = TestBed.createComponent(MatchListComponent);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/matches').flush([]);

    component.onRefresh();

    const req = httpMock.expectOne('http://localhost:8080/api/matches');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
