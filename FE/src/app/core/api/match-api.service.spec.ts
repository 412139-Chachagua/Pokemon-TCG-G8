import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MatchApiService, MatchResponse } from './match-api.service';

describe('MatchApiService', () => {
  let service: MatchApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(MatchApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should list matches with default params', () => {
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

    service.listMatches().subscribe((matches) => {
      expect(matches).toEqual(dummyMatches);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/matches');
    expect(req.request.method).toBe('GET');
    req.flush(dummyMatches);
  });

  it('should list matches with custom status', () => {
    service.listMatches('IN_PROGRESS').subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/matches?status=IN_PROGRESS');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
