import { RESOURCE } from './map.const';

export type Resource = (typeof RESOURCE)[keyof typeof RESOURCE];
export type PieceSeat = 0 | 1 | 2;

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
}

export interface CatanMap {
  readonly hexes: readonly HexField[];
  readonly neighbours: Readonly<Record<number, readonly number[]>>;
  readonly vertices: readonly BoardVertexData[];
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
