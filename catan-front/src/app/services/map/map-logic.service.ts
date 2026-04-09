import {
  DEFAULT_HEX_EDGES,
  DEFAULT_HEX_SPOTS,
  HEX_CIRCUMRADIUS,
  HEX_IDS,
  HEX_NEIGHBOURS,
  RESOURCE,
  RESOURCE_TILE_COLOR,
  STATIC_PRODUCTION_BY_HEX,
  STATIC_RESOURCE_BY_HEX,
} from '../../models/map.const';
import type {
  BoardGraph,
  CatanMap,
  HexField,
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
  const hexes: HexField[] = HEX_IDS.map((id) => ({
    id,
    fieldNumber: id,
    resource: STATIC_RESOURCE_BY_HEX[id] ?? RESOURCE.Desert,
    neighbourHexIds: [...(HEX_NEIGHBOURS[id] ?? [])],
    productionNumber: STATIC_PRODUCTION_BY_HEX[id] ?? null,
    spots: DEFAULT_HEX_SPOTS,
    edges: DEFAULT_HEX_EDGES,
    unavailableSpots: [],
  }));
  return { hexes, neighbours: HEX_NEIGHBOURS };
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
 * Discrete heat tiers by production probability:
 * 2/12 (worst), 3/11, 4/10 (mid), 5/9 (good), 6/8 (best).
 */
export function heatOverlayRgbaByPips(pips: number): string {
  switch (pips) {
    case 5: // 6 / 8 (best)
      return 'rgb(92 142 94)';
    case 4: // 5 / 9
      return 'rgb(86 122 90)';
    case 3: // 4 / 10
      return 'rgb(82 106 87)';
    case 2: // 3 / 11
      return 'rgb(78 90 82)';
    case 1: // 2 / 12
    default: // desert / no token -> map to lowest legend tier
      return 'rgb(72 74 76)';
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
  const order: readonly Resource[] = [RESOURCE.Lumber, RESOURCE.Brick, RESOURCE.Wool, RESOURCE.Grain, RESOURCE.Ore];
  const labels: Record<Resource, string> = {
    [RESOURCE.Lumber]: 'Lumber', [RESOURCE.Brick]: 'Brick', [RESOURCE.Wool]: 'Wool',
    [RESOURCE.Grain]: 'Grain', [RESOURCE.Ore]: 'Ore', [RESOURCE.Desert]: 'Desert',
  };
  const hexes = new Map<Resource, number>();
  const pips = new Map<Resource, number>();
  for (const r of order) { hexes.set(r, 0); pips.set(r, 0); }
  for (const h of map.hexes) {
    if (h.resource === RESOURCE.Desert) continue;
    hexes.set(h.resource, (hexes.get(h.resource) ?? 0) + 1);
    pips.set(h.resource, (pips.get(h.resource) ?? 0) + productionPips(h.productionNumber));
  }
  return order.map((resource) => ({
    resource,
    label: labels[resource],
    hexes: hexes.get(resource) ?? 0,
    pips: pips.get(resource) ?? 0,
    tileColor: RESOURCE_TILE_COLOR[resource],
  }));
}

const SEAT_RESOURCE_WEIGHT: readonly [Record<Resource, number>, Record<Resource, number>, Record<Resource, number>] = [
  { [RESOURCE.Lumber]: 0.19, [RESOURCE.Brick]: 0.17, [RESOURCE.Wool]: 0.19, [RESOURCE.Grain]: 0.23, [RESOURCE.Ore]: 0.22, [RESOURCE.Desert]: 0 },
  { [RESOURCE.Lumber]: 0.21, [RESOURCE.Brick]: 0.19, [RESOURCE.Wool]: 0.2, [RESOURCE.Grain]: 0.21, [RESOURCE.Ore]: 0.19, [RESOURCE.Desert]: 0 },
  { [RESOURCE.Lumber]: 0.22, [RESOURCE.Brick]: 0.18, [RESOURCE.Wool]: 0.22, [RESOURCE.Grain]: 0.19, [RESOURCE.Ore]: 0.19, [RESOURCE.Desert]: 0 },
];

export function analyzePlacement(map: CatanMap): PlacementAnalysis {
  const byId = new Map(map.hexes.map((h) => [h.id, h]));
  const pipsByResource = new Map<Resource, number>([
    [RESOURCE.Lumber, 0], [RESOURCE.Brick, 0], [RESOURCE.Wool, 0], [RESOURCE.Grain, 0], [RESOURCE.Ore, 0],
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
    [RESOURCE.Lumber]: 1, [RESOURCE.Brick]: 1, [RESOURCE.Wool]: 1, [RESOURCE.Grain]: 1, [RESOURCE.Ore]: 1, [RESOURCE.Desert]: 1,
  };
  for (const r of [RESOURCE.Lumber, RESOURCE.Brick, RESOURCE.Wool, RESOURCE.Grain, RESOURCE.Ore] as const) {
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

export function placeRandomPlayerPieces(g: BoardGraph, rng: () => number = Math.random): RandomPlayerPieces {
  const shuffle = <T,>(arr: T[]) => {
    for (let i = arr.length - 1; i > 0; i--) {
      const j = Math.floor(rng() * (i + 1));
      [arr[i], arr[j]] = [arr[j]!, arr[i]!];
    }
  };
  const edgeKey = (a: number, b: number) => `${Math.min(a, b)},${Math.max(a, b)}`;
  const shorten = (x1: number, y1: number, x2: number, y2: number) => {
    const mx = (x1 + x2) / 2, my = (y1 + y2) / 2, t = 0.14;
    return { x1: x1 + (mx - x1) * t, y1: y1 + (my - y1) * t, x2: x2 + (mx - x2) * t, y2: y2 + (my - y2) * t };
  };
  for (let attempt = 0; attempt < 400; attempt++) {
    const verts = Array.from({ length: g.vertexPositions.length }, (_, i) => i);
    shuffle(verts);
    const chosen = new Set<number>();
    for (const v of verts) {
      if (chosen.size >= 6) break;
      let ok = true;
      for (const n of g.adjacency.get(v) ?? []) if (chosen.has(n)) { ok = false; break; }
      if (ok) chosen.add(v);
    }
    if (chosen.size < 6) continue;
    const arr = [...chosen];
    shuffle(arr);
    const bySeat: [number, number][] = [[arr[0]!, arr[1]!], [arr[2]!, arr[3]!], [arr[4]!, arr[5]!]];
    const settlements: SettlementView[] = [];
    for (let seat = 0; seat < 3; seat++) {
      const [a, b] = bySeat[seat]!;
      const pa = g.vertexPositions[a]!, pb = g.vertexPositions[b]!;
      settlements.push({ seat: seat as 0 | 1 | 2, vertexId: a, x: pa.x, y: pa.y }, { seat: seat as 0 | 1 | 2, vertexId: b, x: pb.x, y: pb.y });
    }
    const roads: RoadView[] = [];
    const used = new Set<string>();
    const order: (0 | 1 | 2)[] = [0, 1, 2];
    shuffle(order);
    let okRoads = true;
    for (const seat of order) {
      const candidates: [number, number][] = [];
      for (const v of bySeat[seat]!) {
        for (const u of g.adjacency.get(v) ?? []) {
          const k = edgeKey(u, v);
          if (!used.has(k)) candidates.push([Math.min(u, v), Math.max(u, v)]);
        }
      }
      shuffle(candidates);
      if (candidates.length < 2) { okRoads = false; break; }
      for (let i = 0; i < 2; i++) {
        const [u, v] = candidates[i]!;
        used.add(edgeKey(u, v));
        const p1 = g.vertexPositions[u]!, p2 = g.vertexPositions[v]!;
        const seg = shorten(p1.x, p1.y, p2.x, p2.y);
        roads.push({ seat, v1: u, v2: v, x1: seg.x1, y1: seg.y1, x2: seg.x2, y2: seg.y2 });
      }
    }
    if (okRoads) return { settlements, roads };
  }
  throw new Error('placeRandomPlayerPieces: failed to place pieces after retries');
}


