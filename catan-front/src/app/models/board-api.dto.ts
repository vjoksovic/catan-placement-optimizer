/**
 * JSON shape from {@code com.example.catan.models.Map} / {@code Field}
 * (Jackson default property names).
 */
export interface GameBoardFieldDto {
  id: number;
  fieldNumber: number;
  resource: string | null;
  neighbours: number[];
  vertices: number[];
  productionValue: number;
}

export interface GameBoardVertexDto {
  id: number;
  fields: number[];
  neighbours: number[];
}

export interface GameBoardDto {
  fields: GameBoardFieldDto[];
  vertices: GameBoardVertexDto[];
}
