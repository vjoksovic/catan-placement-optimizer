import type { Resource } from './map.interface';

export const RESOURCE = {
  Wood: 'wood',
  Brick: 'brick',
  Sheep: 'sheep',
  Wheat: 'wheat',
  Ore: 'ore',
  Desert: 'desert',
} as const;

export const HEX_IDS: readonly number[] = [
  0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
];

export const HEX_NEIGHBOURS: Readonly<Record<number, readonly number[]>> = {
  0: [1, 3, 4],
  1: [0, 2, 4, 5],
  2: [1, 5, 6],
  3: [0, 4, 7, 8],
  4: [0, 1, 3, 5, 8, 9],
  5: [1, 2, 4, 6, 9, 10],
  6: [2, 5, 10, 11],
  7: [3, 8, 12],
  8: [3, 4, 7, 9, 12, 13],
  9: [4, 5, 8, 10, 13, 14],
  10: [5, 6, 9, 11, 14, 15],
  11: [6, 10, 15],
  12: [7, 8, 13, 16],
  13: [8, 9, 12, 14, 16, 17],
  14: [9, 10, 13, 15, 17, 18],
  15: [10, 11, 14, 18],
  16: [12, 13, 17],
  17: [13, 14, 16, 18],
  18: [14, 15, 17],
};

export const DEFAULT_HEX_SPOTS: readonly number[] = [1, 2, 3, 4, 5, 6];
export const DEFAULT_HEX_EDGES: readonly number[] = [1, 2, 3, 4, 5, 6];
export const HEX_CIRCUMRADIUS = 88;
export const HEX_POLYGON_POINTS = (() => {
  const parts: string[] = [];
  for (let k = 0; k < 6; k++) {
    const rad = ((-90 + k * 60) * Math.PI) / 180;
    parts.push(`${HEX_CIRCUMRADIUS * Math.cos(rad)},${HEX_CIRCUMRADIUS * Math.sin(rad)}`);
  }
  return parts.join(' ');
})();

export const STATIC_RESOURCE_BY_HEX: Readonly<Record<number, Resource>> = {
  0: RESOURCE.Wood,
  1: RESOURCE.Sheep,
  2: RESOURCE.Wheat,
  3: RESOURCE.Brick,
  4: RESOURCE.Ore,
  5: RESOURCE.Wheat,
  6: RESOURCE.Sheep,
  7: RESOURCE.Wood,
  8: RESOURCE.Brick,
  9: RESOURCE.Desert,
  10: RESOURCE.Ore,
  11: RESOURCE.Wheat,
  12: RESOURCE.Sheep,
  13: RESOURCE.Wood,
  14: RESOURCE.Brick,
  15: RESOURCE.Ore,
  16: RESOURCE.Wheat,
  17: RESOURCE.Sheep,
  18: RESOURCE.Wood,
};

export const STATIC_PRODUCTION_BY_HEX: Readonly<Record<number, number | null>> = {
  0: 6, 1: 3, 2: 8, 3: 5, 4: 10, 5: 9, 6: 4, 7: 11, 8: 5, 9: null,
  10: 9, 11: 12, 12: 4, 13: 10, 14: 8, 15: 2, 16: 11, 17: 3, 18: 6,
};

/** Sidebar / map shared — muted, cool slate base so fields match UI panels. */
export const RESOURCE_TILE_COLOR: Record<Resource, string> = {
  [RESOURCE.Wood]: '#3a5248',
  [RESOURCE.Brick]: '#7d5a4f',
  [RESOURCE.Sheep]: '#4a6952',
  [RESOURCE.Wheat]: '#8a8244',
  [RESOURCE.Ore]: '#545c64',
  [RESOURCE.Desert]: '#877256',
};

export const HEX_RESOURCE_PATTERN_FILL: Record<Resource, string> = {
  [RESOURCE.Wood]: 'url(#hex-fill-wood)',
  [RESOURCE.Brick]: 'url(#hex-fill-brick)',
  [RESOURCE.Sheep]: 'url(#hex-fill-sheep)',
  [RESOURCE.Wheat]: 'url(#hex-fill-wheat)',
  [RESOURCE.Ore]: 'url(#hex-fill-ore)',
  [RESOURCE.Desert]: 'url(#hex-fill-desert)',
};

function mixRgbHex(from: string, to: string, amountTowardTo: number): string {
  const parse = (hex: string) => {
    const h = hex.startsWith('#') ? hex.slice(1) : hex;
    const n = parseInt(h, 16);
    return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 };
  };
  const a = parse(from);
  const b = parse(to);
  const t = Math.max(0, Math.min(1, amountTowardTo));
  const r = Math.round(a.r + (b.r - a.r) * t);
  const g = Math.round(a.g + (b.g - a.g) * t);
  const bl = Math.round(a.b + (b.b - a.b) * t);
  return `#${[r, g, bl].map((x) => x.toString(16).padStart(2, '0')).join('')}`;
}

/** Muted slate blended into resource hues so fields stay readable but subordinate to vertex heat. */
const HEATMAP_FIELD_MUTE_BG = '#252f38';

/** Solid fills for heatmap mode: same resources as normal play, washed toward slate. */
export const HEX_RESOURCE_FILL_HEATMAP_WASHED: Record<Resource, string> = {
  [RESOURCE.Wood]: mixRgbHex(RESOURCE_TILE_COLOR[RESOURCE.Wood], HEATMAP_FIELD_MUTE_BG, 0.5),
  [RESOURCE.Brick]: mixRgbHex(RESOURCE_TILE_COLOR[RESOURCE.Brick], HEATMAP_FIELD_MUTE_BG, 0.5),
  [RESOURCE.Sheep]: mixRgbHex(RESOURCE_TILE_COLOR[RESOURCE.Sheep], HEATMAP_FIELD_MUTE_BG, 0.5),
  [RESOURCE.Wheat]: mixRgbHex(RESOURCE_TILE_COLOR[RESOURCE.Wheat], HEATMAP_FIELD_MUTE_BG, 0.5),
  [RESOURCE.Ore]: mixRgbHex(RESOURCE_TILE_COLOR[RESOURCE.Ore], HEATMAP_FIELD_MUTE_BG, 0.5),
  [RESOURCE.Desert]: mixRgbHex(RESOURCE_TILE_COLOR[RESOURCE.Desert], HEATMAP_FIELD_MUTE_BG, 0.52),
};

