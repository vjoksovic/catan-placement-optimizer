import type { CatanMap, HexField, Resource } from './map.interface';
import type { GameBoardDto, GameBoardFieldDto } from './board-api.dto';
import { RESOURCE } from './map.const';

const API_TO_UI_RESOURCE: Readonly<Record<string, Resource>> = {
  LUMBER: RESOURCE.Lumber,
  BRICK: RESOURCE.Brick,
  WOOL: RESOURCE.Wool,
  GRAIN: RESOURCE.Grain,
  ORE: RESOURCE.Ore,
  DESERT: RESOURCE.Desert,
};

function asResource(value: string | null): Resource {
  if (value) {
    return API_TO_UI_RESOURCE[value] ?? RESOURCE.Desert;
  }
  return RESOURCE.Desert;
}

function mapField(f: GameBoardFieldDto): HexField {
  const resource = asResource(f.resource);
  const productionNumber =
    resource === RESOURCE.Desert ? null : f.fieldNumber > 0 ? f.fieldNumber : null;
  return {
    id: f.id,
    fieldNumber: f.fieldNumber,
    resource,
    neighbourHexIds: [...f.neighbours],
    productionNumber,
    spots: f.spots.map((n) => n + 1),
    edges: f.edges.map((n) => n + 1),
    unavailableSpots: f.unavailableSpots.map((n) => n + 1),
  };
}

export function boardDtoToCatanMap(dto: GameBoardDto): CatanMap {
  // Keep backend order exactly as sent by API.
  const hexes = dto.fields.map((f) => mapField(f));
  const neighbours: Record<number, readonly number[]> = {};
  for (const h of hexes) {
    neighbours[h.id] = h.neighbourHexIds;
  }
  return { hexes, neighbours };
}
