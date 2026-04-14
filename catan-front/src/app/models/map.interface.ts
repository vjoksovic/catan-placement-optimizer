import { RESOURCE } from './map.const';

export type Resource = (typeof RESOURCE)[keyof typeof RESOURCE];
export type PieceSeat = 0 | 1 | 2;
export type HeatmapRating = 'VERY_LOW' | 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH';

export interface HexField {
  readonly id: number;
  readonly fieldNumber: number;
  readonly resource: Resource;
  readonly neighbourHexIds: readonly number[];
  readonly productionNumber: number | null;
  readonly productionValue: number;
  readonly vertexIds: readonly number[];
}

export interface BoardVertexData {
  readonly id: number;
  readonly fields: readonly number[];
  readonly neighbours: readonly number[];
  /** Present when returned from the Java API (vertex heuristics). */
  readonly isAccessible?: boolean;
  readonly heuristicValue?: number;
  /** Vertex-level production heuristic (not the same as hex token pips). */
  readonly vertexProductionValue?: number;
  readonly resourceDiversityValue?: number;
  readonly numberDiversityValue?: number;
  readonly scarcityValue?: number;
  readonly heatmapRating?: HeatmapRating;
}

/** Seat data from `com.example.catan.models.Player` JSON. */
export interface MapPlayerData {
  readonly id: number;
  readonly playstyle: string;
  readonly productionScore: number;
  readonly diversityScore: number;
  readonly scarcityScore: number;
  readonly totalScore: number;
  readonly isPlaying: boolean;
  readonly settlementVertexIds?: readonly number[];
  /** Road edges as vertex pairs (smaller id first). */
  readonly roads?: readonly (readonly [number, number])[];
}

export interface CatanMap {
  readonly hexes: readonly HexField[];
  readonly neighbours: Readonly<Record<number, readonly number[]>>;
  readonly vertices: readonly BoardVertexData[];
  /** Present on API-generated maps (`Map.players`). */
  readonly players?: readonly MapPlayerData[];
  /** Present on API-generated maps (`Map.production`). */
  readonly productionByResource?: Readonly<Partial<Record<Resource, number>>>;
}

export interface ResourceOnMapRow {
  readonly resource: Resource;
  readonly label: string;
  readonly hexes: number;
  readonly production: number;
  readonly tileColor: string;
}

export interface PlayerHeuristicRow {
  readonly seatLabel: '1st' | '2nd' | '3rd';
  readonly resources: number;
  readonly possibility: number;
  readonly scarcity: number;
  readonly overall: number;
}

export interface PlacementAnalysis {
  readonly players: readonly PlayerHeuristicRow[];
  readonly overallHeat01ByHex: Readonly<Record<number, number>>;
}

export interface GraphPoint {
  readonly x: number;
  readonly y: number;
}

export interface BoardGraph {
  readonly vertexPositions: readonly { x: number; y: number }[];
  readonly hexCornerVertexIds: Readonly<Record<number, readonly number[]>>;
  readonly edges: readonly Readonly<[number, number]>[];
  readonly adjacency: ReadonlyMap<number, ReadonlySet<number>>;
}

export interface SettlementView {
  readonly seat: PieceSeat;
  readonly vertexId: number;
  readonly x: number;
  readonly y: number;
}

export interface RoadView {
  readonly seat: PieceSeat;
  readonly v1: number;
  readonly v2: number;
  readonly x1: number;
  readonly y1: number;
  readonly x2: number;
  readonly y2: number;
}

export interface RandomPlayerPieces {
  readonly settlements: readonly SettlementView[];
  readonly roads: readonly RoadView[];
}
