import { Injectable, computed, inject, signal } from '@angular/core';
import {
  HEX_CIRCUMRADIUS,
  HEX_FILL_HEATMAP_NEUTRAL,
  HEX_POLYGON_POINTS,
  HEX_RESOURCE_PATTERN_FILL,
} from '../../models/map.const';
import { MapDataService } from './map-data.service';
import {
  allHexScreenXY,
  analyzePlacement,
  buildBoardGraph,
  getMapViewBoxString,
  heatOverlayRgbaByPips,
  productionPips,
  resourceRowsOnMap,
  vertexHeatFillByVertexId,
} from './map-logic.service';

const BOARD_GRAPH = buildBoardGraph();

export type NowPlayingSeat = '1st' | '2nd' | '3rd';

@Injectable({ providedIn: 'root' })
export class MapFacadeService {
  private readonly mapData = inject(MapDataService);

  readonly nowPlayingSeats = ['1st', '2nd', '3rd'] as const satisfies readonly NowPlayingSeat[];
  readonly nowPlayingSeat = signal<NowPlayingSeat>('1st');

  readonly nowPlayingCaption = computed(() => {
    const seat = this.nowPlayingSeat();
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
  readonly generateMapLoading = this.mapData.generateMapLoading.asReadonly();
  readonly generateMapFailed = this.mapData.generateMapFailed.asReadonly();
  readonly mapGenerated = this.mapData.mapGenerated;
  private readonly xy = allHexScreenXY();

  readonly selectedHexId = signal<number | null>(null);
  readonly selectedVertexId = signal<number | null>(null);
  readonly heatmapOn = signal(false);

  readonly placementAnalysis = computed(() => analyzePlacement(this.map()));
  readonly resourceOnMap = computed(() => resourceRowsOnMap(this.map()));

  readonly playersByOverall = computed(() => {
    const list = [...this.placementAnalysis().players];
    list.sort((a, b) => b.overall - a.overall);
    return list;
  });

  /** One marker per graph vertex (avoids triple-drawing shared corners). */
  readonly heatmapVertexMarkers = computed(() => {
    const selected = this.selectedVertexId();
    const selectedInfo = this.vertexLookup().get(selected ?? -1) ?? null;
    const vertexRing = new Set<number>();
    if (selectedInfo) {
      vertexRing.add(selectedInfo.id);
      for (const n of selectedInfo.neighbours) vertexRing.add(n);
    }

    const mapVertices = this.map().vertices ?? [];
    const fills = vertexHeatFillByVertexId(BOARD_GRAPH, this.map());
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
            selected: selected === v.id,
            ring: vertexRing.has(v.id),
          };
        })
        .filter((v): v is {
          id: number; x: number; y: number; fill: string; selected: boolean; ring: boolean
        } => v !== null);
    }
    return BOARD_GRAPH.vertexPositions.map((p, id) => ({
      id,
      x: p.x,
      y: p.y,
      fill: fills.get(id) ?? heatOverlayRgbaByPips(1),
      selected: selected === id,
      ring: vertexRing.has(id),
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
    return this.map().hexes.map((h) => {
      const number = h.productionNumber;
      const pips = productionPips(number);
      return {
        id: h.id,
        fieldNumber: h.fieldNumber,
        neighbourHexIds: h.neighbourHexIds,
        x: this.xy[h.id]?.x ?? 0,
        y: this.xy[h.id]?.y ?? 0,
        fill: heatmap ? HEX_FILL_HEATMAP_NEUTRAL : HEX_RESOURCE_PATTERN_FILL[h.resource],
        resource: h.resource,
        productionNumber: number,
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

  toggleHeatmap(): void {
    this.heatmapOn.update((v) => !v);
  }

  setNowPlayingSeat(seat: NowPlayingSeat): void {
    this.nowPlayingSeat.set(seat);
  }

  generateMap(): Promise<void> {
    return this.mapData.generateMap();
  }

  startGame(): void {
    // Hook point for game-start flow (lobby/session) once backend endpoint is ready.
    console.log('start game');
  }

  onSpotClick(fieldNumber: number, spotNum: number, event: Event): void {
    event.stopPropagation();
    console.log('vertex click', { field: fieldNumber, vertex: spotNum });
  }

  onEdgeClick(fieldNumber: number, edgeNum: number, event: Event): void {
    event.stopPropagation();
    console.log('edge click', { field: fieldNumber, edge: edgeNum });
  }
}
