import {
  HEX_CIRCUMRADIUS,
  HEX_IDS,
  HEX_NEIGHBOURS,
  RESOURCE,
  RESOURCE_TILE_COLOR,
} from '../../models/map.const';
import type {
  BoardGraph,
  CatanMap,
  HexField,
  PieceSeat,
  PlacementAnalysis,
  PlayerHeuristicRow,
  RandomPlayerPieces,
  Resource,
  ResourceOnMapRow,
  RoadView,
  SettlementView,
} from '../../models/map.interface';

const ROW_COL_BY_ID: ReadonlyArray<readonly [number, number]> = [
  [0, 1], [0, 2], [0, 3], [1, 0], [1, 1], [1, 2], [1, 3],
  [2, 0], [2, 1], [2, 2], [2, 3], [2, 4], [3, 0], [3, 1], [3, 2], [3, 3],
  [4, 1], [4, 2], [4, 3],
];

const CENTER_OFFSET = (() => {
  let sx = 0;
  let sy = 0;
  for (const [row, col] of ROW_COL_BY_ID) {
    const p = oddRPointyTopXY(row, col);
    sx += p.x;
    sy += p.y;
  }
  return { x: sx / ROW_COL_BY_ID.length, y: sy / ROW_COL_BY_ID.length };
})();

function oddRPointyTopXY(row: number, col: number): { x: number; y: number } {
  return {
    x: HEX_CIRCUMRADIUS * Math.sqrt(3) * (col + 0.5 * (row % 2)),
    y: HEX_CIRCUMRADIUS * 1.5 * row,
  };
}

function hexScreenXY(hexId: number): { x: number; y: number } {
  const slot = ROW_COL_BY_ID[hexId];
  if (!slot) return { x: 0, y: 0 };
  const [row, col] = slot;
  const p = oddRPointyTopXY(row, col);
  return { x: p.x - CENTER_OFFSET.x, y: p.y - CENTER_OFFSET.y };
}

export function allHexScreenXY(): Readonly<Record<number, { x: number; y: number }>> {
  const out: Record<number, { x: number; y: number }> = {};
  for (const id of HEX_IDS) out[id] = hexScreenXY(id);
  return out;
}

export function getMapViewBoxString(padding = 120): string {
  let minX = Infinity; let minY = Infinity; let maxX = -Infinity; let maxY = -Infinity;
  for (const id of HEX_IDS) {
    const { x, y } = hexScreenXY(id);
    minX = Math.min(minX, x - HEX_CIRCUMRADIUS);
    maxX = Math.max(maxX, x + HEX_CIRCUMRADIUS);
    minY = Math.min(minY, y - HEX_CIRCUMRADIUS);
    maxY = Math.max(maxY, y + HEX_CIRCUMRADIUS);
  }
  const w = maxX - minX + 2 * padding;
  const h = maxY - minY + 2 * padding;
  return `${minX - padding} ${minY - padding} ${w} ${h}`;
}

export function createStaticCatanMap(): CatanMap {
  const graph = buildBoardGraph();
  const hexes: HexField[] = HEX_IDS.map((id) => ({
    id,
    fieldNumber: 0,
    resource: RESOURCE.Desert,
    neighbourHexIds: [...(HEX_NEIGHBOURS[id] ?? [])],
    productionNumber: null,
    productionValue: 0,
    vertexIds: [...(graph.hexCornerVertexIds[id] ?? [])],
  }));
  const vertices = graph.vertexPositions.map((_, id) => ({
    id,
    fields: [] as number[],
    neighbours: [...(graph.adjacency.get(id) ?? [])],
  }));
  return { hexes, neighbours: HEX_NEIGHBOURS, vertices };
}

export function productionPips(productionNumber: number | null): number {
  if (productionNumber === null || productionNumber === 7) return 0;
  const table: Record<number, number> = { 2: 1, 12: 1, 3: 2, 11: 2, 4: 3, 10: 3, 5: 4, 9: 4, 6: 5, 8: 5 };
  return table[productionNumber] ?? 0;
}

export function heatOverlayRgbaNormalized(t: number): string {
  const x = Math.max(0, Math.min(1, t));
  // Ordered scale only: dark blue -> dark red (all dark tones).
  const u = Math.pow(x, 0.92);
  const c0: [number, number, number] = [46, 66, 104]; // dark blue
  const c1: [number, number, number] = [71, 63, 95];  // dark indigo
  const c2: [number, number, number] = [93, 58, 88];  // dark violet-red
  const c3: [number, number, number] = [112, 52, 72]; // dark red

  const [r, g, b] = u < 0.33
    ? mixRgb(c0, c1, u / 0.33)
    : u < 0.66
      ? mixRgb(c1, c2, (u - 0.33) / 0.33)
      : mixRgb(c2, c3, (u - 0.66) / 0.34);
  const a = 0.2 + 0.32 * u;
  return `rgba(${r},${g},${b},${a})`;
}

/**
 * Discrete heat tiers (1 = worst … 5 = best). Deep sea slate → blue → teal → jade; chroma and lightness ramp up.
 */
export function heatOverlayRgbaByPips(pips: number): string {
  switch (pips) {
    case 5:
      return 'rgb(74 152 124)';
    case 4:
      return 'rgb(54 122 102)';
    case 3:
      return 'rgb(44 98 108)';
    case 2:
      return 'rgb(36 70 96)';
    case 1:
    default:
      return 'rgb(26 38 48)';
  }
}

function mixRgb(
  a: readonly [number, number, number],
  b: readonly [number, number, number],
  t: number,
): [number, number, number] {
  const x = Math.max(0, Math.min(1, t));
  return [
    Math.round(a[0] + (b[0] - a[0]) * x),
    Math.round(a[1] + (b[1] - a[1]) * x),
    Math.round(a[2] + (b[2] - a[2]) * x),
  ];
}

export function resourceRowsOnMap(map: CatanMap): ResourceOnMapRow[] {
  const order: readonly Resource[] = [RESOURCE.Wood, RESOURCE.Brick, RESOURCE.Sheep, RESOURCE.Wheat, RESOURCE.Ore];
  const labels: Record<Resource, string> = {
    [RESOURCE.Wood]: 'Wood', [RESOURCE.Brick]: 'Brick', [RESOURCE.Sheep]: 'Sheep',
    [RESOURCE.Wheat]: 'Wheat', [RESOURCE.Ore]: 'Ore', [RESOURCE.Desert]: 'Desert',
  };
  const hexes = new Map<Resource, number>();
  const production = new Map<Resource, number>();
  for (const r of order) { hexes.set(r, 0); production.set(r, 0); }
  for (const h of map.hexes) {
    if (h.resource === RESOURCE.Desert) continue;
    hexes.set(h.resource, (hexes.get(h.resource) ?? 0) + 1);
    const backendProduction = map.productionByResource?.[h.resource];
    if (typeof backendProduction !== 'number') {
      production.set(h.resource, (production.get(h.resource) ?? 0) + (h.productionValue ?? 0));
    }
  }
  if (map.productionByResource) {
    for (const resource of order) {
      const backendValue = map.productionByResource[resource];
      if (typeof backendValue === 'number') {
        production.set(resource, backendValue);
      }
    }
  }
  return order.map((resource) => ({
    resource,
    label: labels[resource],
    hexes: hexes.get(resource) ?? 0,
    production: production.get(resource) ?? 0,
    tileColor: RESOURCE_TILE_COLOR[resource],
  }));
}

const SEAT_RESOURCE_WEIGHT: readonly [Record<Resource, number>, Record<Resource, number>, Record<Resource, number>] = [
  { [RESOURCE.Wood]: 0.19, [RESOURCE.Brick]: 0.17, [RESOURCE.Sheep]: 0.19, [RESOURCE.Wheat]: 0.23, [RESOURCE.Ore]: 0.22, [RESOURCE.Desert]: 0 },
  { [RESOURCE.Wood]: 0.21, [RESOURCE.Brick]: 0.19, [RESOURCE.Sheep]: 0.2, [RESOURCE.Wheat]: 0.21, [RESOURCE.Ore]: 0.19, [RESOURCE.Desert]: 0 },
  { [RESOURCE.Wood]: 0.22, [RESOURCE.Brick]: 0.18, [RESOURCE.Sheep]: 0.22, [RESOURCE.Wheat]: 0.19, [RESOURCE.Ore]: 0.19, [RESOURCE.Desert]: 0 },
];

export function analyzePlacement(map: CatanMap): PlacementAnalysis {
  const byId = new Map(map.hexes.map((h) => [h.id, h]));
  const pipsByResource = new Map<Resource, number>([
    [RESOURCE.Wood, 0], [RESOURCE.Brick, 0], [RESOURCE.Sheep, 0], [RESOURCE.Wheat, 0], [RESOURCE.Ore, 0],
  ]);
  for (const id of HEX_IDS) {
    const h = byId.get(id);
    if (!h || h.resource === RESOURCE.Desert) continue;
    pipsByResource.set(h.resource, (pipsByResource.get(h.resource) ?? 0) + productionPips(h.productionNumber));
  }
  let totalPips = 0;
  for (const p of pipsByResource.values()) totalPips += p;
  const avg = totalPips > 0 ? totalPips / 5 : 1;
  const scarcity: Record<Resource, number> = {
    [RESOURCE.Wood]: 1, [RESOURCE.Brick]: 1, [RESOURCE.Sheep]: 1, [RESOURCE.Wheat]: 1, [RESOURCE.Ore]: 1, [RESOURCE.Desert]: 1,
  };
  for (const r of [RESOURCE.Wood, RESOURCE.Brick, RESOURCE.Sheep, RESOURCE.Wheat, RESOURCE.Ore] as const) {
    scarcity[r] = Math.min(2.4, Math.max(0.65, avg / ((pipsByResource.get(r) ?? 0) + 0.25)));
  }

  const rawRes: [number, number, number] = [0, 0, 0];
  const rawPoss: [number, number, number] = [0, 0, 0];
  const rawScarc: [number, number, number] = [0, 0, 0];
  const rawHeat: Record<number, [number, number, number]> = {};

  for (const id of HEX_IDS) {
    const h = byId.get(id);
    if (!h) continue;
    const pip = productionPips(h.productionNumber);
    if (h.resource === RESOURCE.Desert || pip <= 0) {
      rawHeat[id] = [0, 0, 0];
      continue;
    }
    let neighSum = 0;
    for (const nid of map.neighbours[id] ?? []) neighSum += productionPips(byId.get(nid)?.productionNumber ?? null);
    rawHeat[id] = [0, 0, 0];
    for (let seat = 0; seat < 3; seat++) {
      const base = pip * SEAT_RESOURCE_WEIGHT[seat][h.resource];
      rawRes[seat] += base;
      rawPoss[seat] += base * (1 + 0.38 * (neighSum / 22));
      rawScarc[seat] += base * scarcity[h.resource];
      rawHeat[id][seat] = base * scarcity[h.resource];
    }
  }

  const maxOf = (a: [number, number, number]) => Math.max(a[0], a[1], a[2], 1e-9);
  const resources = rawRes.map((v) => Math.round(100 * (v / maxOf(rawRes)))) as [number, number, number];
  const possibility = rawPoss.map((v) => Math.round(100 * (v / maxOf(rawPoss)))) as [number, number, number];
  const scarc = rawScarc.map((v) => Math.round(100 * (v / maxOf(rawScarc)))) as [number, number, number];
  const labels: readonly ('1st' | '2nd' | '3rd')[] = ['1st', '2nd', '3rd'];
  const players: PlayerHeuristicRow[] = [0, 1, 2].map((i) => ({
    seatLabel: labels[i], resources: resources[i], possibility: possibility[i], scarcity: scarc[i],
    overall: Math.round(0.34 * resources[i] + 0.33 * possibility[i] + 0.33 * scarc[i]),
  }));

  const rawOverall: Record<number, number> = {};
  for (const id of HEX_IDS) {
    const t = rawHeat[id];
    rawOverall[id] = t ? (t[0] + t[1] + t[2]) / 3 : 0;
  }
  let minO = Infinity; let maxO = -Infinity;
  for (const id of HEX_IDS) {
    const v = rawOverall[id] ?? 0;
    if (v <= 0) continue;
    minO = Math.min(minO, v);
    maxO = Math.max(maxO, v);
  }
  if (minO === Infinity) minO = 0;
  if (maxO === -Infinity) maxO = 0;
  const span = maxO - minO;
  const overallHeat01ByHex: Record<number, number> = {};
  for (const id of HEX_IDS) {
    const v = rawOverall[id] ?? 0;
    overallHeat01ByHex[id] = v <= 0 ? 0 : span < 1e-9 ? 0.5 : (v - minO) / span;
  }
  return { players, overallHeat01ByHex };
}

/** Heatmap vertex grade 1 (worst) … 5 (best), aligned with {@link heatOverlayRgbaByPips}. */
export type VertexHeatGrade = 1 | 2 | 3 | 4 | 5;

/**
 * Per-vertex grade 1–5: API heatmap ratings, or pip-sum normalization on static maps.
 */
export function vertexHeatGradeByVertexId(
  graph: BoardGraph,
  map: CatanMap,
): ReadonlyMap<number, VertexHeatGrade> {
  const ratingToGrade: Record<'VERY_LOW' | 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH', VertexHeatGrade> = {
    VERY_LOW: 1,
    LOW: 2,
    MEDIUM: 3,
    HIGH: 4,
    VERY_HIGH: 5,
  };
  if ((map.vertices ?? []).length > 0 && map.vertices.some((v) => !!v.heatmapRating)) {
    const grades = new Map<number, VertexHeatGrade>();
    for (const v of map.vertices) {
      if (v.heatmapRating) {
        grades.set(v.id, ratingToGrade[v.heatmapRating] ?? 3);
      }
    }
    return grades;
  }

  const byId = new Map(map.hexes.map((h) => [h.id, h]));
  const totals = new Map<number, number>();

  for (const hid of HEX_IDS) {
    const h = byId.get(hid);
    const corners = h?.vertexIds?.length ? h.vertexIds : graph.hexCornerVertexIds[hid];
    if (!corners) continue;
    const pip = productionPips(h?.productionNumber ?? null);
    for (let k = 0; k < 6; k++) {
      const vid = corners[k]!;
      totals.set(vid, (totals.get(vid) ?? 0) + pip);
    }
  }

  let minT = Infinity;
  let maxT = -Infinity;
  for (const t of totals.values()) {
    if (t <= 0) continue;
    minT = Math.min(minT, t);
    maxT = Math.max(maxT, t);
  }
  if (minT === Infinity) minT = 0;
  if (maxT === -Infinity) maxT = 0;
  const span = maxT - minT;

  const grades = new Map<number, VertexHeatGrade>();
  for (const [vid, t] of totals) {
    let tier: VertexHeatGrade;
    if (t <= 0) {
      tier = 1;
    } else if (span < 1e-9) {
      tier = 3;
    } else {
      const norm = (t - minT) / span;
      const idx = Math.min(4, Math.floor(norm * 5));
      tier = (idx + 1) as VertexHeatGrade;
    }
    grades.set(vid, tier);
  }
  return grades;
}

/**
 * Sum of adjacent hex production pips per board vertex (settlement intersection),
 * then min–max normalized and mapped to the same 5-tier colors as field pips.
 */
export function vertexHeatFillByVertexId(graph: BoardGraph, map: CatanMap): ReadonlyMap<number, string> {
  const grades = vertexHeatGradeByVertexId(graph, map);
  const fills = new Map<number, string>();
  for (const [vid, g] of grades) {
    fills.set(vid, heatOverlayRgbaByPips(g));
  }
  return fills;
}

/**
 * First hex (by id) that contains this vertex → field number and corner spot 1–6 (for UI labels).
 */
export function fieldSpotForVertex(
  vertexId: number,
  map: CatanMap,
  graph: BoardGraph,
): { fieldNumber: number; spot: number } | null {
  const hexes = [...map.hexes].sort((a, b) => a.id - b.id);
  for (const h of hexes) {
    const corners = h.vertexIds?.length ? [...h.vertexIds] : graph.hexCornerVertexIds[h.id];
    if (!corners?.length) continue;
    const idx = corners.indexOf(vertexId);
    if (idx >= 0) {
      return { fieldNumber: h.fieldNumber, spot: idx + 1 };
    }
  }
  return null;
}

export function buildBoardGraph(): BoardGraph {
  const keyToId = new Map<string, number>();
  const vertexPositions: { x: number; y: number }[] = [];
  const hexCornerVertexIds: Record<number, number[]> = {};
  const getOrCreate = (x: number, y: number) => {
    const k = `${Math.round(x * 1000) / 1000},${Math.round(y * 1000) / 1000}`;
    let id = keyToId.get(k);
    if (id === undefined) {
      id = vertexPositions.length;
      keyToId.set(k, id);
      vertexPositions.push({ x, y });
    }
    return id;
  };
  for (const hid of HEX_IDS) {
    const c = hexScreenXY(hid);
    const corners: number[] = [];
    for (let k = 0; k < 6; k++) {
      const rad = ((-90 + k * 60) * Math.PI) / 180;
      corners.push(getOrCreate(c.x + HEX_CIRCUMRADIUS * Math.cos(rad), c.y + HEX_CIRCUMRADIUS * Math.sin(rad)));
    }
    hexCornerVertexIds[hid] = corners;
  }
  const edgeSet = new Set<string>();
  for (const hid of HEX_IDS) {
    const c = hexCornerVertexIds[hid]!;
    for (let k = 0; k < 6; k++) {
      const a = c[k]!, b = c[(k + 1) % 6]!;
      const u = Math.min(a, b), v = Math.max(a, b);
      edgeSet.add(`${u},${v}`);
    }
  }
  const edges: [number, number][] = [];
  const adjacency = new Map<number, Set<number>>();
  for (const key of edgeSet) {
    const [a, b] = key.split(',').map(Number) as [number, number];
    edges.push([a, b]);
    if (!adjacency.has(a)) adjacency.set(a, new Set());
    if (!adjacency.has(b)) adjacency.set(b, new Set());
    adjacency.get(a)!.add(b);
    adjacency.get(b)!.add(a);
  }
  const roAdj = new Map<number, ReadonlySet<number>>();
  for (const [k, v] of adjacency) roAdj.set(k, v);
  return { vertexPositions, hexCornerVertexIds, edges, adjacency: roAdj };
}

/**
 * Settlement/road SVG data from {@link CatanMap.players} only:
 * {@link MapPlayerData.settlementVertexIds} and {@link MapPlayerData.roads} — not from vertex neighbour flags.
 */
export function piecesFromApiMap(map: CatanMap, g: BoardGraph): RandomPlayerPieces | null {
  return piecesFromApiMapWithLimit(map, g);
}

const PLACEMENT_ORDER: readonly PieceSeat[] = [0, 1, 2, 2, 1, 0];

export function piecesFromApiMapWithLimit(
  map: CatanMap,
  g: BoardGraph,
  maxPlacements?: number,
): RandomPlayerPieces | null {
  const players = map.players;
  if (!players?.length) {
    return null;
  }
  const hasSettlementData = players.some(
    (p) => p.settlementVertexIds && p.settlementVertexIds.length > 0,
  );
  if (!hasSettlementData) {
    return null;
  }
  const shorten = (x1: number, y1: number, x2: number, y2: number) => {
    const mx = (x1 + x2) / 2;
    const my = (y1 + y2) / 2;
    const t = 0.14;
    return {
      x1: x1 + (mx - x1) * t,
      y1: y1 + (my - y1) * t,
      x2: x2 + (mx - x2) * t,
      y2: y2 + (my - y2) * t,
    };
  };
  const settlements: SettlementView[] = [];
  const roads: RoadView[] = [];
  const roadKeys = new Set<string>(roads.map((r) => `${Math.min(r.v1, r.v2)}-${Math.max(r.v1, r.v2)}`));
  const vertexById = new Map((map.vertices ?? []).map((v) => [v.id, v] as const));
  const visiblePlacements = orderedVisiblePlacements(players, maxPlacements);
  for (const placement of visiblePlacements) {
    const seat = placement.seat;
    const vid = placement.vertexId;
    const pos = g.vertexPositions[vid];
    if (!pos) {
      continue;
    }
    settlements.push({ seat, vertexId: vid, x: pos.x, y: pos.y });
    const neighbour = roadNeighbourForVertex(vid, vertexById);
    if (neighbour === null) {
      continue;
    }
    const v1 = Math.min(vid, neighbour);
    const v2 = Math.max(vid, neighbour);
    const roadKey = `${v1}-${v2}`;
    if (roadKeys.has(roadKey)) {
      continue;
    }
    const pt1 = g.vertexPositions[v1];
    const pt2 = g.vertexPositions[v2];
    if (!pt1 || !pt2) {
      continue;
    }
    const seg = shorten(pt1.x, pt1.y, pt2.x, pt2.y);
    roads.push({ seat, v1, v2, x1: seg.x1, y1: seg.y1, x2: seg.x2, y2: seg.y2 });
    roadKeys.add(roadKey);
  }
  if (!settlements.length && !roads.length) {
    return null;
  }
  return { settlements, roads };
}

function roadNeighbourForVertex(
  vertexId: number,
  vertexById: ReadonlyMap<number, { neighbourRoadFlags?: Readonly<Record<number, boolean>> }>,
): number | null {
  const vertex = vertexById.get(vertexId);
  const flags = vertex?.neighbourRoadFlags;
  if (!flags) {
    return null;
  }
  for (const [nKey, hasRoad] of Object.entries(flags)) {
    if (hasRoad !== true) {
      continue;
    }
    const n = Number(nKey);
    if (!Number.isNaN(n)) {
      return n;
    }
  }
  return null;
}

function orderedVisiblePlacements(
  players: readonly { id: number; settlementVertexIds?: readonly number[] }[],
  maxPlacements?: number,
): ReadonlyArray<{ seat: PieceSeat; vertexId: number }> {
  const bySeat: Record<PieceSeat, readonly number[]> = { 0: [], 1: [], 2: [] };
  for (const p of players) {
    const seat = p.id as PieceSeat;
    if (seat >= 0 && seat <= 2) {
      bySeat[seat] = p.settlementVertexIds ?? [];
    }
  }
  const takenBySeat: Record<PieceSeat, number> = { 0: 0, 1: 0, 2: 0 };
  const sequence: { seat: PieceSeat; vertexId: number }[] = [];
  for (const seat of PLACEMENT_ORDER) {
    const idx = takenBySeat[seat];
    const vid = bySeat[seat][idx];
    if (vid === undefined) {
      continue;
    }
    sequence.push({ seat, vertexId: vid });
    takenBySeat[seat] = idx + 1;
  }
  if (maxPlacements === undefined || maxPlacements < 0) {
    return sequence;
  }
  return sequence.slice(0, maxPlacements);
}

