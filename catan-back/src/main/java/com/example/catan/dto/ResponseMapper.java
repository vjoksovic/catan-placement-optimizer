package com.example.catan.dto;

import java.util.ArrayList;
import java.util.List;

import com.example.catan.models.map.Field;
import com.example.catan.models.map.Map;
import com.example.catan.models.map.Player;
import com.example.catan.models.map.Vertex;
import com.example.catan.models.values.Heuristic;

public final class ResponseMapper {

  private ResponseMapper() {
  }

  public static Response from(Map map) {
    Response response = new Response();
    response.setFields(mapFields(map.getFields()));
    response.setVertices(mapVertices(map.getVertices()));
    response.setPlayers(mapPlayers(map.getPlayers()));
    response.setProduction(map.getProduction());
    return response;
  }

  private static List<FieldDto> mapFields(List<Field> fields) {
    List<FieldDto> out = new ArrayList<>(fields.size());
    for (Field field : fields) {
      FieldDto dto = new FieldDto();
      dto.setId(field.getId());
      dto.setFieldNumber(field.getFieldNumber());
      dto.setResource(field.getResource());
      dto.setNeighbours(field.getNeighbours());
      dto.setVertices(field.getVertices());
      dto.setProductionValue(field.getProductionValue());
      out.add(dto);
    }
    return out;
  }

  private static List<VertexDto> mapVertices(List<Vertex> vertices) {
    List<VertexDto> out = new ArrayList<>(vertices.size());
    for (Vertex vertex : vertices) {
      VertexDto dto = new VertexDto();
      dto.setId(vertex.getId());
      dto.setFields(vertex.getFields());
      dto.setRoadFlags(vertex.getRoadFlags());
      dto.setSettled(vertex.isSettled());
      dto.setHeatmapRating(vertex.getHeatmapRating());
      dto.setValue(mapHeuristic(vertex.getValue()));
      out.add(dto);
    }
    return out;
  }

  private static HeuristicDto mapHeuristic(Heuristic heuristic) {
    HeuristicDto dto = new HeuristicDto();
    dto.setProductionValue(heuristic.getProductionValue());
    dto.setResourceDiversityValue(heuristic.getResourceDiversityValue());
    dto.setNumberDiversityValue(heuristic.getNumberDiversityValue());
    dto.setScarcityValue(heuristic.getScarcityValue());
    dto.setBalancedValue(heuristic.getBalancedValue());
    dto.setProductionFocusedValue(heuristic.getProductionFocusedValue());
    dto.setScarcityFocusedValue(heuristic.getScarcityFocusedValue());
    dto.setOverallValue(heuristic.getOverallValue());
    return dto;
  }

  private static List<PlayerDto> mapPlayers(List<Player> players) {
    List<PlayerDto> out = new ArrayList<>(players.size());
    for (Player player : players) {
      PlayerDto dto = new PlayerDto();
      dto.setId(player.getId());
      dto.setPlaystyle(player.getPlaystyle());
      dto.setSettlements(player.getSettlements());
      dto.setScore(player.getScore());
      out.add(dto);
    }
    return out;
  }
}
