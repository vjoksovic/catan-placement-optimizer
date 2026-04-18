import { Injectable, computed, inject, signal } from '@angular/core';
import {
  HEX_CIRCUMRADIUS,
  HEX_POLYGON_POINTS,
  HEX_RESOURCE_FILL_HEATMAP_WASHED,
  HEX_RESOURCE_PATTERN_FILL,
  RESOURCE,
} from '../../models/map.const';
import { MapDataService } from './map-data.service';
import type { TacticId } from '../../models/tactic';
import {
  allHexScreenXY,
  analyzePlacement,
  buildBoardGraph,
  getMapViewBoxString,
  heatOverlayRgbaByPips,
  productionPips,
  resourceRowsOnMap,
  vertexHeatFillByVertexId,
  vertexHeatGradeByVertexId,
} from './map-logic.service';
import type { PlayerHeuristicRow } from '../../models/map.interface';

const BOARD_GRAPH = buildBoardGraph();
const NEUTRAL_FIELD_FILL = 'rgba(88, 106, 118, 0.55)';
const UI_SESSION_STORAGE_KEY = 'catan.session.ui.v1';

type PersistedUiState = {
  nowPlayingSeat: NowPlayingSeat;
  heatmapOn: boolean;
  gameStarted: boolean;
};

export type NowPlayingSeat = '1st' | '2nd' | '3rd';
const TURN_ORDER_TO_SEAT: readonly NowPlayingSeat[] = ['1st', '2nd', '3rd', '3rd', '2nd', '1st'];

@Injectable({ providedIn: 'root' })
export class MapFacadeService {
  private readonly mapData = inject(MapDataService);

  readonly nowPlayingSeats = ['1st', '2nd', '3rd'] as const satisfies readonly NowPlayingSeat[];
  readonly nowPlayingSeat = signal<NowPlayingSeat>('1st');

  /** True after the user starts a session from this UI (until abort or full reload). */
  readonly gameStarted = signal(false);

  /** Active seat computed from current frontend turn number (0..5). */
  readonly nowPlayingFromTurn = computed((): NowPlayingSeat | null => {
    const turn = this.mapData.activeTurnNumber();
    if (turn === null || turn < 0 || turn >= TURN_ORDER_TO_SEAT.length) {
      return null;
    }
    return TURN_ORDER_TO_SEAT[turn] ?? null;
  });

  /** UI seat: frontend turn-derived seat when available, otherwise local selection. */
  readonly nowPlayingSeatEffective = computed(
    () => this.nowPlayingFromTurn() ?? this.nowPlayingSeat(),
  );

  /**
   * After "Start game", manual seat toggles are disabled only when the API marks an active player.
   * Before start, the now-playing block is hidden so this does not apply.
   */
  readonly nowPlayingSeatChoiceLocked = computed(
    () => this.gameStarted() && this.nowPlayingFromTurn() !== null,
  );

  readonly nowPlayingCaption = computed(() => {
    const seat = this.nowPlayingSeatEffective();
    if (seat === '1st') {
      return { seat: '1st', colorLabel: 'White', swatch: 'white' as const };
    }
    if (seat === '2nd') {
      return { seat: '2nd', colorLabel: 'Blue', swatch: 'blue' as const };
    }
    return { seat: '3rd', colorLabel: 'Orange', swatch: 'orange' as const };
  });

  readonly map = this.mapData.map;
  readonly playerPieces = this.mapData.playerPieces;
  readonly hasSettlementsOnMap = computed(() => this.playerPieces().settlements.length > 0);
  readonly placementRevealInProgress = this.mapData.placementRevealInProgress;
  readonly gameOver = this.mapData.gameOver;
  readonly generateMapLoading = this.mapData.generateMapLoading.asReadonly();
  readonly generateMapFailed = this.mapData.generateMapFailed.asReadonly();
  readonly mapGenerated = this.mapData.mapGenerated;

  /** Disabled only while generating. */
  readonly generateMapDisabled = computed(
    () => this.generateMapLoading(),
  );

  readonly startGameDisabled = computed(
    () =>
      this.generateMapLoading() ||
      !this.mapGenerated() ||
      this.gameStarted(),
  );

  private readonly xy = allHexScreenXY();

  readonly selectedHexId = signal<number | null>(null);
  readonly selectedVertexId = signal<number | null>(null);
  readonly heatmapOn = signal(false);

  /** Screen coordinates for vertex heuristic tooltip (heatmap dots). */
  private readonly vertexHeatTooltip = signal<{
    vertexId: number;
    screenX: number;
    screenY: number;
  } | null>(null);

  constructor() {
    this.restoreUiFromSession();
  }

  /** Prod / Res / Num / Scar + Overall (one decimal). */
  readonly vertexHeatTooltipView = computed(() => {
    const t = this.vertexHeatTooltip();
    if (!t || !this.heatmapOn()) {
      return null;
    }
    const v = this.map().vertices?.find((x) => x.id === t.vertexId);
    const fmt = (n: number | undefined) => {
      if (n === undefined || Number.isNaN(Number(n))) {
        return '—';
      }
      return (Math.round(Number(n) * 10) / 10).toFixed(1);
    };
    const rows = [
      { label: 'Prod', value: fmt(v?.vertexProductionValue), highlighted: false },
      { label: 'Res', value: fmt(v?.resourceDiversityValue), highlighted: false },
      { label: 'Num', value: fmt(v?.numberDiversityValue), highlighted: false },
      { label: 'Scar', value: fmt(v?.scarcityValue), highlighted: false },
      { label: 'Overall', value: fmt(v?.overallValue), highlighted: true },
    ] as const;
    const placeBelow = t.screenY < 110;
    return {
      screenX: t.screenX,
      screenY: t.screenY,
      placeBelow,
      rows,
    };
  });

  readonly placementAnalysis = computed(() => analyzePlacement(this.map()));
  readonly resourceOnMap = computed(() => resourceRowsOnMap(this.map()));

  private readonly playersFromBackend = computed(() => {
    const players = this.map().players ?? [];
    if (!players.length) {
      return null;
    }
    const seatLabelById: Record<number, '1st' | '2nd' | '3rd'> = {
      0: '1st',
      1: '2nd',
      2: '3rd',
    };
    const rows: PlayerHeuristicRow[] = players
      .map((p) => {
        const seatLabel = seatLabelById[p.id];
        if (!seatLabel) {
          return null;
        }
        return {
          seatLabel,
          possibility: p.productionScore, // "Prod" column
          resources: p.diversityScore, // "Div" column
          scarcity: p.scarcityScore,
          overall: p.totalScore,
        };
      })
      .filter((r): r is PlayerHeuristicRow => r !== null);
    return rows.length ? rows : null;
  });

  readonly playersByOverall = computed(() => {
    const list = [...(this.playersFromBackend() ?? this.placementAnalysis().players)];
    list.sort((a, b) => b.overall - a.overall);
    return list;
  });

  readonly winnerCaption = computed(() => {
    const winner = this.playersByOverall()[0];
    if (!winner) {
      return null;
    }
    if (winner.seatLabel === '1st') {
      return { seat: '1st', colorLabel: 'White', swatch: 'white' as const };
    }
    if (winner.seatLabel === '2nd') {
      return { seat: '2nd', colorLabel: 'Blue', swatch: 'blue' as const };
    }
    return { seat: '3rd', colorLabel: 'Orange', swatch: 'orange' as const };
  });

  readonly playersBySeat = computed(() => {
    const order: Record<'1st' | '2nd' | '3rd', number> = { '1st': 0, '2nd': 1, '3rd': 2 };
    const list = [...(this.playersFromBackend() ?? this.placementAnalysis().players)];
    list.sort((a, b) => order[a.seatLabel] - order[b.seatLabel]);
    return list;
  });

  /** One marker per graph vertex (avoids triple-drawing shared corners). */
  readonly heatmapVertexMarkers = computed(() => {
    const selected = this.selectedVertexId();
    const selectedInfo = this.vertexLookup().get(selected ?? -1) ?? null;
    const occupiedVertices = new Set<number>();
    for (const p of this.playerPieces().settlements) {
      occupiedVertices.add(p.vertexId);
    }
    const vertexRing = new Set<number>();
    if (selectedInfo) {
      vertexRing.add(selectedInfo.id);
      for (const n of selectedInfo.neighbours) vertexRing.add(n);
    }

    const mapVertices = this.map().vertices ?? [];
    const mapRef = this.map();
    const fills = vertexHeatFillByVertexId(BOARD_GRAPH, mapRef);
    const grades = vertexHeatGradeByVertexId(BOARD_GRAPH, mapRef);
    if (mapVertices.length > 0) {
      return mapVertices
        .map((v) => {
          const p = BOARD_GRAPH.vertexPositions[v.id];
          if (!p) return null;
          return {
            id: v.id,
            x: p.x,
            y: p.y,
            fill: fills.get(v.id) ?? heatOverlayRgbaByPips(1),
            grade: (grades.get(v.id) ?? 1) as 1 | 2 | 3 | 4 | 5,
            selected: selected === v.id,
            ring: vertexRing.has(v.id),
            occupied: occupiedVertices.has(v.id),
          };
        })
        .filter((v): v is {
          id: number;
          x: number;
          y: number;
          fill: string;
          grade: 1 | 2 | 3 | 4 | 5;
          selected: boolean;
          ring: boolean;
          occupied: boolean;
        } => v !== null);
    }
    return BOARD_GRAPH.vertexPositions.map((p, id) => ({
      id,
      x: p.x,
      y: p.y,
      fill: fills.get(id) ?? heatOverlayRgbaByPips(1),
      grade: (grades.get(id) ?? 1) as 1 | 2 | 3 | 4 | 5,
      selected: selected === id,
      ring: vertexRing.has(id),
      occupied: occupiedVertices.has(id),
    }));
  });

  readonly hexPolygonPoints = HEX_POLYGON_POINTS;

  private readonly localSpotGeometry = (() => {
    const radius = HEX_CIRCUMRADIUS;
    return Array.from({ length: 6 }, (_, i) => {
      const num = i + 1;
      const rad = ((-90 + i * 60) * Math.PI) / 180;
      return { num, x: radius * Math.cos(rad), y: radius * Math.sin(rad) };
    });
  })();

  private readonly localEdgeGeometry = this.localSpotGeometry.map((s, i) => {
    const next = this.localSpotGeometry[(i + 1) % 6]!;
    return { num: i + 1, x1: s.x, y1: s.y, x2: next.x, y2: next.y };
  });

  readonly mapViewBox = getMapViewBoxString(120);

  private readonly vertexLookup = computed(() => {
    const out = new Map<number, { id: number; neighbours: readonly number[]; fields: readonly number[] }>();
    const m = this.map();
    if ((m.vertices ?? []).length > 0) {
      for (const v of m.vertices) {
        out.set(v.id, { id: v.id, neighbours: v.neighbours, fields: v.fields });
      }
    } else {
      for (const [id, neighbours] of BOARD_GRAPH.adjacency) {
        const fields = m.hexes
          .filter((h) => (h.vertexIds ?? []).includes(id))
          .map((h) => h.id);
        out.set(id, { id, neighbours: [...neighbours], fields });
      }
    }
    return out;
  });

  readonly hexViews = computed(() => {
    const selected = this.selectedHexId();
    const selectedVertex = this.selectedVertexId();
    const selectedVertexInfo = this.vertexLookup().get(selectedVertex ?? -1) ?? null;
    const lit = new Set<number>();
    if (selected !== null) {
      lit.add(selected);
      for (const n of this.map().neighbours[selected] ?? []) {
        lit.add(n);
      }
    }
    const vertexFieldSet = new Set<number>(selectedVertexInfo?.fields ?? []);

    const heatmap = this.heatmapOn();
    const generated = this.mapGenerated();
    const resourceFills = heatmap ? HEX_RESOURCE_FILL_HEATMAP_WASHED : HEX_RESOURCE_PATTERN_FILL;
    return this.map().hexes.map((h) => {
      const number = h.productionNumber;
      const pips = productionPips(number);
      return {
        id: h.id,
        fieldNumber: h.fieldNumber,
        neighbourHexIds: h.neighbourHexIds,
        x: this.xy[h.id]?.x ?? 0,
        y: this.xy[h.id]?.y ?? 0,
        fill: generated ? (resourceFills[h.resource] ?? resourceFills[RESOURCE.Desert]) : NEUTRAL_FIELD_FILL,
        resource: h.resource,
        productionNumber: number,
        showToken: !(generated && h.resource === RESOURCE.Desert && number === null),
        pips,
        tokenHot: number === 6 || number === 8,
        lit: lit.has(h.id),
        selected: selected === h.id,
        vertexLinked: vertexFieldSet.has(h.id),
        spots: (h.vertexIds?.length ? h.vertexIds : BOARD_GRAPH.hexCornerVertexIds[h.id] ?? [])
          .map((vertexId, i) => {
            const p = this.localSpotGeometry[i];
            return p ? { num: i + 1, x: p.x, y: p.y, vertexId } : null;
          })
          .filter((v): v is { num: number; x: number; y: number; vertexId: number } => !!v),
        edges: this.localEdgeGeometry
          .filter(
            (e): e is { num: number; x1: number; y1: number; x2: number; y2: number } => !!e,
          ),
      };
    });
  });

  onHexClick(id: number): void {
    this.selectedHexId.update((cur) => (cur === id ? null : id));
  }

  onVertexClick(id: number, event: Event): void {
    event.stopPropagation();
    this.selectedVertexId.update((cur) => (cur === id ? null : id));
  }

  onVertexHeatPointerEnter(id: number, ev: PointerEvent): void {
    if (!this.heatmapOn()) {
      return;
    }
    this.vertexHeatTooltip.set({
      vertexId: id,
      screenX: ev.clientX,
      screenY: ev.clientY,
    });
  }

  onVertexHeatPointerMove(id: number, ev: PointerEvent): void {
    const cur = this.vertexHeatTooltip();
    if (!cur || cur.vertexId !== id) {
      return;
    }
    this.vertexHeatTooltip.set({
      vertexId: id,
      screenX: ev.clientX,
      screenY: ev.clientY,
    });
  }

  onVertexHeatPointerLeave(): void {
    this.vertexHeatTooltip.set(null);
  }

  toggleHeatmap(): void {
    this.heatmapOn.update((v) => !v);
    this.vertexHeatTooltip.set(null);
    this.persistUiToSession();
  }

  setNowPlayingSeat(seat: NowPlayingSeat): void {
    this.nowPlayingSeat.set(seat);
    this.persistUiToSession();
  }

  async generateMap(tactics: readonly TacticId[]): Promise<void> {
    await this.mapData.generateMap(tactics);
    if (!this.mapData.generateMapFailed()) {
      this.gameStarted.set(false);
      this.persistUiToSession();
    }
  }

  async startGame(): Promise<void> {
    await this.mapData.startGame();
    if (!this.mapData.generateMapFailed()) {
      this.gameStarted.set(true);
      this.persistUiToSession();
    }
  }

  abortPlacing(): void {
    this.mapData.abortPlacementReveal();
    this.gameStarted.set(false);
    this.persistUiToSession();
  }

  onSpotClick(fieldNumber: number, spotNum: number, event: Event): void {
    event.stopPropagation();
    console.log('vertex click', { field: fieldNumber, vertex: spotNum });
  }

  onEdgeClick(fieldNumber: number, edgeNum: number, event: Event): void {
    event.stopPropagation();
    console.log('edge click', { field: fieldNumber, edge: edgeNum });
  }

  private restoreUiFromSession(): void {
    if (typeof window === 'undefined') {
      return;
    }
    try {
      const raw = window.sessionStorage.getItem(UI_SESSION_STORAGE_KEY);
      if (!raw) {
        return;
      }
      const parsed = JSON.parse(raw) as Partial<PersistedUiState>;
      if (parsed.nowPlayingSeat === '1st' || parsed.nowPlayingSeat === '2nd' || parsed.nowPlayingSeat === '3rd') {
        this.nowPlayingSeat.set(parsed.nowPlayingSeat);
      }
      this.heatmapOn.set(Boolean(parsed.heatmapOn));
      this.gameStarted.set(Boolean(parsed.gameStarted));
    } catch {
      // Ignore invalid persisted UI state.
    }
  }

  private persistUiToSession(): void {
    if (typeof window === 'undefined') {
      return;
    }
    const payload: PersistedUiState = {
      nowPlayingSeat: this.nowPlayingSeat(),
      heatmapOn: this.heatmapOn(),
      gameStarted: this.gameStarted(),
    };
    try {
      window.sessionStorage.setItem(UI_SESSION_STORAGE_KEY, JSON.stringify(payload));
    } catch {
      // Ignore storage restrictions.
    }
  }
}
