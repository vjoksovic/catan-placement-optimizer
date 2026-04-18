import type { BoardVertexData, CatanMap, HexField, MapPlayerData, Resource } from './map.interface';
import type {
  GameBoardDto,
  GameBoardFieldDto,
  GameBoardPlayerDto,
  GameBoardVertexDto,
  VertexRoadFlagsDto,
  VertexNeighboursDto,
} from './board-api.dto';
import { RESOURCE } from './map.const';

/** Java enum names, lowercase UI tokens, and pre-rename names (persisted JSON / old clients). */
const RESOURCE_ALIASES: Readonly<Record<string, Resource>> = {
  WOOD: RESOURCE.Wood,
  wood: RESOURCE.Wood,
  LUMBER: RESOURCE.Wood,
  lumber: RESOURCE.Wood,
  BRICK: RESOURCE.Brick,
  brick: RESOURCE.Brick,
  SHEEP: RESOURCE.Sheep,
  sheep: RESOURCE.Sheep,
  WOOL: RESOURCE.Sheep,
  wool: RESOURCE.Sheep,
  WHEAT: RESOURCE.Wheat,
  wheat: RESOURCE.Wheat,
  GRAIN: RESOURCE.Wheat,
  grain: RESOURCE.Wheat,
  ORE: RESOURCE.Ore,
  ore: RESOURCE.Ore,
  DESERT: RESOURCE.Desert,
  desert: RESOURCE.Desert,
};

const CANONICAL_RESOURCE_SET = new Set<string>(Object.values(RESOURCE));

function isKnownResourceKey(key: string): boolean {
  return Object.prototype.hasOwnProperty.call(RESOURCE_ALIASES, key) || CANONICAL_RESOURCE_SET.has(key);
}

export function resourceFromApiOrPersisted(raw: string | null | undefined): Resource {
  if (raw == null || raw === '') {
    return RESOURCE.Desert;
  }
  const mapped = RESOURCE_ALIASES[raw];
  if (mapped !== undefined) {
    return mapped;
  }
  if (CANONICAL_RESOURCE_SET.has(raw)) {
    return raw as Resource;
  }
  return RESOURCE.Desert;
}

/**
 * Fixes hex {@code resource} and {@code productionByResource} keys after resource renames
 * (e.g. localStorage still has lumber/wool/grain).
 */
export function normalizeCatanMapResources(map: CatanMap): CatanMap {
  const hexes = map.hexes.map((h) => ({
    ...h,
    resource: resourceFromApiOrPersisted(h.resource as unknown as string),
  }));
  let productionByResource = map.productionByResource;
  if (productionByResource && Object.keys(productionByResource).length > 0) {
    const next: Partial<Record<Resource, number>> = {};
    for (const [key, value] of Object.entries(productionByResource)) {
      if (!isKnownResourceKey(key)) {
        continue;
      }
      const r = resourceFromApiOrPersisted(key);
      if (typeof value === 'number') {
        next[r] = (next[r] ?? 0) + value;
      }
    }
    productionByResource = next;
  }
  return { ...map, hexes, productionByResource };
}

function asResource(value: string | null): Resource {
  return resourceFromApiOrPersisted(value);
}

function mapField(f: GameBoardFieldDto): HexField {
  const resource = asResource(f.resource);
  const productionNumber =
    resource === RESOURCE.Desert ? null : f.fieldNumber > 0 ? f.fieldNumber : null;
  const productionValue = typeof f.productionValue === 'number'
    ? f.productionValue
    : productionNumber === null
      ? 0
      : ({ 2: 1, 12: 1, 3: 2, 11: 2, 4: 3, 10: 3, 5: 4, 9: 4, 6: 5, 8: 5 }[productionNumber] ?? 0);
  return {
    id: f.id,
    fieldNumber: f.fieldNumber,
    resource,
    neighbourHexIds: [...f.neighbours],
    productionNumber,
    productionValue,
    vertexIds: [...f.vertices],
  };
}

/**
 * Backend may send adjacency as {@code number[]} or as a map of vertexId → boolean.
 * Keys are always used as graph adjacency ids.
 */
function vertexNeighbourIds(
  neighbours: VertexNeighboursDto | undefined,
  roadFlags: VertexRoadFlagsDto | undefined,
): number[] {
  if (Array.isArray(neighbours)) {
    return neighbours.map((n) => Number(n));
  }
  const source = neighbours && !Array.isArray(neighbours) ? neighbours : roadFlags;
  if (!source) {
    return [];
  }
  return Object.keys(source)
    .map((k) => Number(k))
    .filter((n) => !Number.isNaN(n))
    .sort((a, b) => a - b);
}

function neighbourRoadFlagsFromDto(
  roadFlags: VertexRoadFlagsDto | undefined,
  neighbours: VertexNeighboursDto | undefined,
): Readonly<Record<number, boolean>> | undefined {
  const source = roadFlags ?? (neighbours && !Array.isArray(neighbours) ? neighbours : undefined);
  if (!source) {
    return undefined;
  }
  const out: Record<number, boolean> = {};
  for (const [k, v] of Object.entries(source)) {
    const id = Number(k);
    if (!Number.isNaN(id)) {
      out[id] = v;
    }
  }
  return Object.keys(out).length ? out : undefined;
}

function settlementVertexIdsFromPlayer(p: GameBoardPlayerDto): number[] | undefined {
  if (Array.isArray(p.settlementVertexIds) && p.settlementVertexIds.length) {
    const ids = p.settlementVertexIds.map((x) => Number(x)).filter((x) => !Number.isNaN(x));
    return ids.length ? ids : undefined;
  }
  const raw = p.settlements;
  if (!Array.isArray(raw) || !raw.length) {
    return undefined;
  }
  const ids: number[] = [];
  for (const item of raw) {
    if (typeof item === 'number' && Number.isFinite(item)) {
      ids.push(item);
    } else if (item && typeof item === 'object' && 'id' in item) {
      const id = Number((item as { id: unknown }).id);
      if (!Number.isNaN(id)) {
        ids.push(id);
      }
    }
  }
  return ids.length ? ids : undefined;
}

function roadPairsFromPlayer(p: GameBoardPlayerDto): [number, number][] | undefined {
  if (!Array.isArray(p.roads) || !p.roads.length) {
    return undefined;
  }
  const out: [number, number][] = [];
  for (const row of p.roads) {
    if (!Array.isArray(row) || row.length < 2) {
      continue;
    }
    const a = Number(row[0]);
    const b = Number(row[1]);
    if (Number.isNaN(a) || Number.isNaN(b)) {
      continue;
    }
    out.push(a <= b ? [a, b] : [b, a]);
  }
  return out.length ? out : undefined;
}

function mapVertex(v: GameBoardVertexDto): BoardVertexData {
  const neighbourRoadFlags = neighbourRoadFlagsFromDto(v.roadFlags, v.neighbours);
  const base: BoardVertexData = {
    id: v.id,
    fields: [...(v.fields ?? [])],
    neighbours: vertexNeighbourIds(v.neighbours, v.roadFlags),
    ...(neighbourRoadFlags ? { neighbourRoadFlags } : {}),
  };
  const val = v.value;
  const production = v.productionValue ?? val?.productionValue;
  const resDiv = v.resourceDiversityValue ?? val?.resourceDiversityValue;
  const numDiv = v.numberDiversityValue ?? val?.numberDiversityValue;
  const scarcity = v.scarcityValue ?? val?.scarcityValue;
  const balanced = val?.balancedValue;
  const productionFocused = val?.productionFocusedValue;
  const scarcityFocused = val?.scarcityFocusedValue;
  const overall = val?.overallValue ?? val?.totalValue;
  const hasHeuristic =
    v.isAccessible !== undefined
    || v.heuristicValue !== undefined
    || production !== undefined
    || resDiv !== undefined
    || numDiv !== undefined
    || scarcity !== undefined
    || balanced !== undefined
    || productionFocused !== undefined
    || scarcityFocused !== undefined
    || overall !== undefined;
  if (!hasHeuristic) {
    return base;
  }
  return {
    ...base,
    isAccessible: v.isAccessible,
    heuristicValue: v.heuristicValue,
    vertexProductionValue: production,
    resourceDiversityValue: resDiv,
    numberDiversityValue: numDiv,
    scarcityValue: scarcity,
    balancedValue: balanced,
    productionFocusedValue: productionFocused,
    scarcityFocusedValue: scarcityFocused,
    overallValue: overall,
    heatmapRating: v.heatmapRating,
  };
}

function mapPlayer(p: GameBoardPlayerDto): MapPlayerData {
  const score = p.score ?? {};
  const settlementVertexIds = settlementVertexIdsFromPlayer(p);
  const roads = roadPairsFromPlayer(p);
  return {
    id: p.id,
    tactic: String(p.tactic),
    productionScore: score.productionScore ?? p.productionScore ?? 0,
    diversityScore: score.diversityScore ?? p.diversityScore ?? 0,
    scarcityScore: score.scarcityScore ?? p.scarcityScore ?? 0,
    totalScore: score.totalScore ?? p.totalScore ?? 0,
    isPlaying: p.playing ?? p.isPlaying ?? false,
    ...(settlementVertexIds ? { settlementVertexIds } : {}),
    ...(roads ? { roads } : {}),
  };
}

function mapProductionByResource(
  production: Record<string, number> | undefined,
): Partial<Record<Resource, number>> | undefined {
  if (!production) {
    return undefined;
  }
  const out: Partial<Record<Resource, number>> = {};
  for (const [key, value] of Object.entries(production)) {
    if (!isKnownResourceKey(key)) {
      continue;
    }
    const resource = resourceFromApiOrPersisted(key);
    const n = typeof value === 'number' ? value : 0;
    out[resource] = (out[resource] ?? 0) + n;
  }
  return out;
}

export function boardDtoToCatanMap(dto: GameBoardDto): CatanMap {
  // Keep backend order exactly as sent by API.
  const hexes = dto.fields.map((f) => mapField(f));
  const neighbours: Record<number, readonly number[]> = {};
  for (const h of hexes) {
    neighbours[h.id] = h.neighbourHexIds;
  }
  const vertices = (dto.vertices ?? []).map((v) => mapVertex(v));
  const players = dto.players?.length ? dto.players.map((p) => mapPlayer(p)) : undefined;
  const productionByResource = mapProductionByResource(dto.production);
  return {
    hexes,
    neighbours,
    vertices,
    ...(players ? { players } : {}),
    ...(productionByResource ? { productionByResource } : {}),
  };
}
