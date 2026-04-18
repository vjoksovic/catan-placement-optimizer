import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import type { Observable } from 'rxjs';
import type { GameBoardDto, GameTurnResponseDto } from '../../models/board-api.dto';
import type { TacticId } from '../../models/tactic';

@Injectable({ providedIn: 'root' })
export class MapApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080';

  generateMap(tactics: readonly TacticId[]): Observable<GameBoardDto> {
    return this.http.post<GameBoardDto>(`${this.baseUrl}/api/maps/generate`, { tactics });
  }

  playTurn(turnNumber: number): Observable<GameTurnResponseDto> {
    return this.http.post<GameTurnResponseDto>(`${this.baseUrl}/api/game/turn`, { turnNumber });
  }
}
