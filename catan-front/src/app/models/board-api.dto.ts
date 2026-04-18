/**
 * JSON shape from {@code com.example.catan.models.Map} (Jackson default names).
 * Vertex {@code neighbours} may be a JSON array or a string-keyed object ({@code HashMap<Integer, Boolean>} on the server);
 * the UI uses adjacency keys only. Settlements and roads are read from {@link GameBoardPlayerDto} only.
 */
export interface GameBoardFieldDto {
  id: number;
  fieldNumber: number;
  resource: string | null;
  neighbours: number[];
  vertices: number[];
  productionValue: number;
}

export type VertexNeighboursDto = number[] | Record<string, boolean>;
export type VertexRoadFlagsDto = Record<string, boolean>;

/** Jackson serializes {@code Vertex.value} ({@code Heuristic}) as this object. */
export interface GameBoardVertexHeuristicDto {
  productionValue?: number;
  resourceDiversityValue?: number;
  numberDiversityValue?: number;
  scarcityValue?: number;
  balancedValue?: number;
  productionFocusedValue?: number;
  scarcityFocusedValue?: number;
  overallValue?: number;
  /** Legacy name kept for backward compatibility. */
  totalValue?: number;
}

export interface GameBoardVertexDto {
  id: number;
  fields: number[];
  /** Legacy adjacency payload (array or map). */
  neighbours?: VertexNeighboursDto;
  /** Preferred payload: adjacent vertex id -> road exists on edge. */
  roadFlags?: VertexRoadFlagsDto;
  /** Nested heuristic payload from Java {@code Vertex.value}. */
  value?: GameBoardVertexHeuristicDto;
  heatmapRating?: 'VERY_LOW' | 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH';
  isAccessible?: boolean;
  heuristicValue?: number;
  /** Flattened or duplicate of {@link value.productionValue} from API. */
  productionValue?: number;
  resourceDiversityValue?: number;
  numberDiversityValue?: number;
  scarcityValue?: number;
}

export interface GameBoardPlayerDto {
  id: number;
  tactic: string;
  /** Preferred: vertex ids with settlements (two per player after placement). */
  settlementVertexIds?: number[];
  /** @deprecated Full vertex objects may appear from older serializers — ids are extracted in the mapper. */
  settlements?: unknown[];
  /** Road edges as {@code [v1, v2]} with {@code v1 <= v2} (two per player). */
  roads?: number[][];
  score?: {
    productionScore?: number;
    diversityScore?: number;
    scarcityScore?: number;
    totalScore?: number;
  };
  /** Legacy flat shape fallback. */
  productionScore?: number;
  diversityScore?: number;
  scarcityScore?: number;
  totalScore?: number;
  /** Jackson often serializes {@code isPlaying} as {@code playing}. */
  isPlaying?: boolean;
  playing?: boolean;
}

export interface GameBoardDto {
  fields: GameBoardFieldDto[];
  vertices: GameBoardVertexDto[];
  players?: GameBoardPlayerDto[];
  /** Resource production totals (enum name -> production sum). */
  production?: Record<string, number>;
}

export interface GameTurnResponseDto {
  map: GameBoardDto;
  nextTurnNumber: number | null;
}
