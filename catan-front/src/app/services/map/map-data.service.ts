import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { boardDtoToCatanMap } from '../../models/board.mapper';
import type { CatanMap } from '../../models/map.interface';
import {
  createStaticCatanMap,
  buildBoardGraph,
  placeRandomPlayerPieces,
  allHexScreenXY,
} from './map-logic.service';
import { HEX_CIRCUMRADIUS } from '../../models/map.const';
import { MapApiService } from './map-api.service';

@Injectable({ providedIn: 'root' })
export class MapDataService {
  private static readonly GENERATED_MAP_STORAGE_KEY = 'catan.generated.map';
  private readonly mapApi = inject(MapApiService);
  private readonly mapState = signal<CatanMap>(this.loadInitialMap());
  readonly map = this.mapState.asReadonly();
  readonly generateMapLoading = signal(false);
  readonly generateMapFailed = signal(false);
  readonly playerPieces = computed(() => this.createPlayerPieces(this.mapState()));

  async generateMap(): Promise<void> {
    this.generateMapLoading.set(true);
    this.generateMapFailed.set(false);
    try {
      const dto = await firstValueFrom(this.mapApi.generateMap());
      const generatedMap = boardDtoToCatanMap(dto);
      this.mapState.set(generatedMap);
      this.clearSavedGeneratedMap();
      this.saveGeneratedMap(generatedMap);
    } catch {
      this.generateMapFailed.set(true);
    } finally {
      this.generateMapLoading.set(false);
    }
  }

  private loadInitialMap(): CatanMap {
    const savedMap = this.loadSavedGeneratedMap();
    return savedMap ?? createStaticCatanMap();
  }

  private saveGeneratedMap(map: CatanMap): void {
    const storage = this.getBrowserStorage();
    if (!storage) return;
    storage.setItem(MapDataService.GENERATED_MAP_STORAGE_KEY, JSON.stringify(map));
  }

  private loadSavedGeneratedMap(): CatanMap | null {
    const storage = this.getBrowserStorage();
    if (!storage) return null;
    const raw = storage.getItem(MapDataService.GENERATED_MAP_STORAGE_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as CatanMap;
    } catch {
      storage.removeItem(MapDataService.GENERATED_MAP_STORAGE_KEY);
      return null;
    }
  }

  private clearSavedGeneratedMap(): void {
    const storage = this.getBrowserStorage();
    if (!storage) return;
    storage.removeItem(MapDataService.GENERATED_MAP_STORAGE_KEY);
  }

  private getBrowserStorage(): Storage | null {
    if (typeof window === 'undefined') {
      return null;
    }
    return window.localStorage;
  }

  private createPlayerPieces(map: CatanMap) {
    const xy = allHexScreenXY();
    const pieces = placeRandomPlayerPieces(buildBoardGraph());
    const firstHexId = map.hexes[0]?.id ?? 1;
    const pinned = this.worldSpot(firstHexId, 3, xy);
    let placed = false;
    const settlements = pieces.settlements.map((s) => {
      if (!placed && s.seat === 0) {
        placed = true;
        return { ...s, x: pinned.x, y: pinned.y };
      }
      return s;
    });
    return { ...pieces, settlements };
  }

  private worldSpot(
    fieldNumber: number,
    spotNum: number,
    xy: Readonly<Record<number, { x: number; y: number }>>,
  ): { x: number; y: number } {
    const c = xy[fieldNumber] ?? { x: 0, y: 0 };
    const i = Math.max(1, Math.min(6, spotNum)) - 1;
    const rad = ((-90 + i * 60) * Math.PI) / 180;
    return {
      x: c.x + HEX_CIRCUMRADIUS * Math.cos(rad),
      y: c.y + HEX_CIRCUMRADIUS * Math.sin(rad),
    };
  }
}

