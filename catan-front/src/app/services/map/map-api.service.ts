import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import type { Observable } from 'rxjs';
import type { GameBoardDto } from '../../models/board-api.dto';

@Injectable({ providedIn: 'root' })
export class MapApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080';

  generateMap(): Observable<GameBoardDto> {
    return this.http.get<GameBoardDto>(`${this.baseUrl}/api/maps/generate`);
  }
}
