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
import type { TacticId } from '../../models/tactic';

const MAP_SESSION_STORAGE_KEY = 'catan.session.mapData.v1';

type PersistedMapData = {
  map: CatanMap;
  mapGenerated: boolean;
  activeTurnNumber: number | null;
  gameOver: boolean;
};

@Injectable({ providedIn: 'root' })
export class MapDataService {
  private readonly mapApi = inject(MapApiService);
  private readonly hasGeneratedMap = signal(false);
  private readonly mapState = signal<CatanMap>(createStaticCatanMap());
  readonly map = this.mapState.asReadonly();
  /** True after a successful API generate or when a saved generated map was restored. */
  readonly mapGenerated = this.hasGeneratedMap.asReadonly();
  readonly generateMapLoading = signal(false);
  readonly generateMapFailed = signal(false);
  /** Bumped in {@link stopTurnLoop} so in-flight turn chains exit between API calls. */
  private turnLoopGeneration = 0;
  private readonly placementRevealInProgressState = signal(false);
  readonly placementRevealInProgress = this.placementRevealInProgressState.asReadonly();
  private readonly activeTurnNumberState = signal<number | null>(null);
  readonly activeTurnNumber = this.activeTurnNumberState.asReadonly();
  private readonly gameOverState = signal(false);
  readonly gameOver = this.gameOverState.asReadonly();
  readonly playerPieces = computed(() => this.createPlayerPieces(this.mapState()));

  constructor() {
    this.restoreFromSession();
  }

  async generateMap(tactics: readonly TacticId[]): Promise<void> {
    this.stopTurnLoop();
    this.activeTurnNumberState.set(null);
    this.gameOverState.set(false);
    this.generateMapLoading.set(true);
    this.generateMapFailed.set(false);
    try {
      const dto = await firstValueFrom(this.mapApi.generateMap(tactics));
      const generatedMap = boardDtoToCatanMap(dto);
      this.mapState.set(generatedMap);
      this.hasGeneratedMap.set(true);
      this.persistToSession();
    } catch {
      this.generateMapFailed.set(true);
    } finally {
      this.generateMapLoading.set(false);
    }
  }

  async startGame(): Promise<void> {
    this.stopTurnLoop();
    this.generateMapFailed.set(false);
    this.placementRevealInProgressState.set(true);
    this.gameOverState.set(false);
    const generation = this.turnLoopGeneration;
    await this.playTurnAndSchedule(0, generation);
  }

  abortPlacementReveal(): void {
    this.stopTurnLoop();
  }

  private createPlayerPieces(map: CatanMap) {
    const graph = buildBoardGraph();
    const fromApi = piecesFromApiMap(map, graph);
    if (fromApi) {
      return fromApi;
    }
    return { settlements: [], roads: [] };
  }

  private async playTurnAndSchedule(
    turnNumber: number,
    generation: number,
  ): Promise<void> {
    if (generation !== this.turnLoopGeneration) {
      return;
    }
    try {
      this.activeTurnNumberState.set(turnNumber);
      const dto = await firstValueFrom(this.mapApi.playTurn(turnNumber));
      if (generation !== this.turnLoopGeneration) {
        return;
      }
      const nextMap = boardDtoToCatanMap(dto.map);
      this.mapState.set(nextMap);
      this.hasGeneratedMap.set(true);
      this.persistToSession();
      if (dto.nextTurnNumber === null) {
        this.gameOverState.set(true);
        this.stopTurnLoop();
        return;
      }
      if (generation !== this.turnLoopGeneration) {
        return;
      }
      await this.playTurnAndSchedule(dto.nextTurnNumber as number, generation);
    } catch {
      this.generateMapFailed.set(true);
      this.stopTurnLoop();
    }
  }

  private stopTurnLoop(): void {
    this.turnLoopGeneration++;
    this.placementRevealInProgressState.set(false);
    this.persistToSession();
  }

  private restoreFromSession(): void {
    if (typeof window === 'undefined') {
      return;
    }
    try {
      const raw = window.sessionStorage.getItem(MAP_SESSION_STORAGE_KEY);
      if (!raw) {
        return;
      }
      const parsed = JSON.parse(raw) as Partial<PersistedMapData>;
      if (!parsed || typeof parsed !== 'object' || !parsed.map) {
        return;
      }
      const restoredMap = normalizeCatanMapResources(parsed.map as CatanMap);
      this.mapState.set(restoredMap);
      this.hasGeneratedMap.set(Boolean(parsed.mapGenerated));
      const turn = typeof parsed.activeTurnNumber === 'number' ? parsed.activeTurnNumber : null;
      this.activeTurnNumberState.set(turn);
      this.gameOverState.set(Boolean(parsed.gameOver));
      // Timers cannot survive browser reloads.
      this.placementRevealInProgressState.set(false);
    } catch {
      // Ignore invalid session payloads.
    }
  }

  private persistToSession(): void {
    if (typeof window === 'undefined') {
      return;
    }
    const payload: PersistedMapData = {
      map: this.mapState(),
      mapGenerated: this.hasGeneratedMap(),
      activeTurnNumber: this.activeTurnNumberState(),
      gameOver: this.gameOverState(),
    };
    try {
      window.sessionStorage.setItem(MAP_SESSION_STORAGE_KEY, JSON.stringify(payload));
    } catch {
      // Ignore storage quota/session restrictions.
    }
  }
}

