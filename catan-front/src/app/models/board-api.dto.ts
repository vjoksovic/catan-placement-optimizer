/**
 * JSON shape from {@code com.example.catan.models.Map} / {@code Field}
 * (Jackson default property names).
 */
export interface GameBoardFieldDto {
  id: number;
  fieldNumber: number;
  resource: string | null;
  neighbours: number[];
  spots: number[];
  edges: number[];
  unavailableSpots: number[];
}

export interface GameBoardDto {
  fields: GameBoardFieldDto[];
}
