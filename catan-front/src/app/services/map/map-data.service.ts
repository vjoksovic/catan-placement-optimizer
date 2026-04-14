import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { boardDtoToCatanMap, normalizeCatanMapResources } from '../../models/board.mapper';
import type { CatanMap } from '../../models/map.interface';
import {
  createStaticCatanMap,
  buildBoardGraph,
  piecesFromApiMap,
} from './map-logic.service';
import { MapApiService } from './map-api.service';
import type { PlaystyleId } from '../../models/playstyle';

@Injectable({ providedIn: 'root' })
export class MapDataService {
  private static readonly GENERATED_MAP_STORAGE_KEY = 'catan.generated.map';
  private readonly mapApi = inject(MapApiService);
  private readonly hasGeneratedMap = signal(false);
  private readonly mapState = signal<CatanMap>(this.loadInitialMap());
  readonly map = this.mapState.asReadonly();
  /** True after a successful API generate or when a saved generated map was restored. */
  readonly mapGenerated = this.hasGeneratedMap.asReadonly();
  readonly generateMapLoading = signal(false);
  readonly generateMapFailed = signal(false);
  readonly playerPieces = computed(() => this.createPlayerPieces(this.mapState()));

  async generateMap(playstyles: readonly PlaystyleId[]): Promise<void> {
    this.generateMapLoading.set(true);
    this.generateMapFailed.set(false);
    try {
      const dto = await firstValueFrom(this.mapApi.generateMap(playstyles));
      const generatedMap = boardDtoToCatanMap(dto);
      this.mapState.set(generatedMap);
      this.hasGeneratedMap.set(true);
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
    if (savedMap) {
      this.hasGeneratedMap.set(true);
      return savedMap;
    }
    this.hasGeneratedMap.set(false);
    return createStaticCatanMap();
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
      return normalizeCatanMapResources(JSON.parse(raw) as CatanMap);
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
    const graph = buildBoardGraph();
    const fromApi = piecesFromApiMap(map, graph);
    if (fromApi) {
      return fromApi;
    }
    return { settlements: [], roads: [] };
  }
}

