import { Injectable, computed, inject, signal } from '@angular/core';
import { HEX_CIRCUMRADIUS, HEX_POLYGON_POINTS, HEX_RESOURCE_PATTERN_FILL } from '../../models/map.const';
import { MapDataService } from './map-data.service';
import {
  allHexScreenXY,
  analyzePlacement,
  getMapViewBoxString,
  heatOverlayRgbaByPips,
  productionPips,
  resourceRowsOnMap,
} from './map-logic.service';

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
  private readonly xy = allHexScreenXY();

  readonly selectedHexId = signal<number | null>(null);
  readonly heatmapOn = signal(false);

  readonly placementAnalysis = computed(() => analyzePlacement(this.map()));
  readonly resourceOnMap = computed(() => resourceRowsOnMap(this.map()));

  readonly playersByOverall = computed(() => {
    const list = [...this.placementAnalysis().players];
    list.sort((a, b) => b.overall - a.overall);
    return list;
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

  readonly hexViews = computed(() => {
    const selected = this.selectedHexId();
    const lit = new Set<number>();
    if (selected !== null) {
      lit.add(selected);
      for (const n of this.map().neighbours[selected] ?? []) {
        lit.add(n);
      }
    }

    const analysis = this.placementAnalysis();
    return this.map().hexes.map((h) => {
      const number = h.productionNumber;
      const pips = productionPips(number);
      const heat01 = analysis.overallHeat01ByHex[h.id] ?? 0;
      return {
        id: h.id,
        fieldNumber: h.fieldNumber,
        neighbourHexIds: h.neighbourHexIds,
        x: this.xy[h.id]?.x ?? 0,
        y: this.xy[h.id]?.y ?? 0,
        fill: HEX_RESOURCE_PATTERN_FILL[h.resource],
        resource: h.resource,
        productionNumber: number,
        pips,
        heatFill: heatOverlayRgbaByPips(pips),
        tokenHot: number === 6 || number === 8,
        lit: lit.has(h.id),
        selected: selected === h.id,
        spots: h.spots
          .map((num) => this.localSpotGeometry[num - 1])
          .filter((v): v is { num: number; x: number; y: number } => !!v),
        edges: h.edges
          .map((num) => this.localEdgeGeometry[num - 1])
          .filter(
            (e): e is { num: number; x1: number; y1: number; x2: number; y2: number } => !!e,
          ),
      };
    });
  });

  onHexClick(id: number): void {
    this.selectedHexId.update((cur) => (cur === id ? null : id));
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

  onSpotClick(fieldNumber: number, spotNum: number, event: Event): void {
    event.stopPropagation();
    console.log('vertex click', { field: fieldNumber, vertex: spotNum });
  }

  onEdgeClick(fieldNumber: number, edgeNum: number, event: Event): void {
    event.stopPropagation();
    console.log('edge click', { field: fieldNumber, edge: edgeNum });
  }
}
