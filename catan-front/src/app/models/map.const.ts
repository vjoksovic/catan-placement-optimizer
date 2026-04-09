import type { Resource } from './map.interface';

export const RESOURCE = {
  Lumber: 'lumber',
  Brick: 'brick',
  Wool: 'wool',
  Grain: 'grain',
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
  0: RESOURCE.Lumber,
  1: RESOURCE.Wool,
  2: RESOURCE.Grain,
  3: RESOURCE.Brick,
  4: RESOURCE.Ore,
  5: RESOURCE.Grain,
  6: RESOURCE.Wool,
  7: RESOURCE.Lumber,
  8: RESOURCE.Brick,
  9: RESOURCE.Desert,
  10: RESOURCE.Ore,
  11: RESOURCE.Grain,
  12: RESOURCE.Wool,
  13: RESOURCE.Lumber,
  14: RESOURCE.Brick,
  15: RESOURCE.Ore,
  16: RESOURCE.Grain,
  17: RESOURCE.Wool,
  18: RESOURCE.Lumber,
};

export const STATIC_PRODUCTION_BY_HEX: Readonly<Record<number, number | null>> = {
  0: 6, 1: 3, 2: 8, 3: 5, 4: 10, 5: 9, 6: 4, 7: 11, 8: 5, 9: null,
  10: 9, 11: 12, 12: 4, 13: 10, 14: 8, 15: 2, 16: 11, 17: 3, 18: 6,
};

export const RESOURCE_TILE_COLOR: Record<Resource, string> = {
  [RESOURCE.Lumber]: '#3d5c4f',
  [RESOURCE.Brick]: '#8f675c',
  [RESOURCE.Wool]: '#5e7a62',
  [RESOURCE.Grain]: '#9c8f52',
  [RESOURCE.Ore]: '#6f767e',
  [RESOURCE.Desert]: '#a68f6b',
};

export const HEX_RESOURCE_PATTERN_FILL: Record<Resource, string> = {
  [RESOURCE.Lumber]: 'url(#hex-fill-lumber)',
  [RESOURCE.Brick]: 'url(#hex-fill-brick)',
  [RESOURCE.Wool]: 'url(#hex-fill-wool)',
  [RESOURCE.Grain]: 'url(#hex-fill-grain)',
  [RESOURCE.Ore]: 'url(#hex-fill-ore)',
  [RESOURCE.Desert]: 'url(#hex-fill-desert)',
};

