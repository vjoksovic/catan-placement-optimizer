import type { BoardVertexData, CatanMap, HexField, Resource } from './map.interface';
import type { GameBoardDto, GameBoardFieldDto, GameBoardVertexDto } from './board-api.dto';
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

function mapVertex(v: GameBoardVertexDto): BoardVertexData {
  return {
    id: v.id,
    fields: [...v.fields],
    neighbours: [...v.neighbours],
  };
}

export function boardDtoToCatanMap(dto: GameBoardDto): CatanMap {
  // Keep backend order exactly as sent by API.
  const hexes = dto.fields.map((f) => mapField(f));
  const neighbours: Record<number, readonly number[]> = {};
  for (const h of hexes) {
    neighbours[h.id] = h.neighbourHexIds;
  }
  const vertices = (dto.vertices ?? []).map((v) => mapVertex(v));
  return { hexes, neighbours, vertices };
}
