package com.example.catan.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.catan.models.map.Map;
import com.example.catan.models.map.Player;
import com.example.catan.models.map.Vertex;
import com.example.catan.models.values.Heuristic;
import com.example.catan.utils.ConfigLoader;
import com.example.catan.utils.MathUtil;

import com.example.catan.models.map.Field;
import com.example.catan.models.enums.HeatmapRating;
import org.springframework.stereotype.Service;

@Service
public class MapService {

  private final VertexService vertexService;
  private final FieldService fieldService;
  private final MathUtil.HeuristicScalingContext scalingContext;
  
  public MapService(VertexService vertexService, FieldService fieldService) {
    this.vertexService = vertexService;
    this.fieldService = fieldService;
    java.util.Map<String, Double> maxValues = ConfigLoader.loadHeuristicScalingMaxValues();
    java.util.Map<String, Double> targetShares = ConfigLoader.loadHeuristicScalingTargetShares();
    this.scalingContext = MathUtil.buildHeuristicScalingContext(maxValues, targetShares);
  }

  public void processPlayerHeuristics(Map map) {
    for (Player player : map.getPlayers()) {
      processPlayerVertices(map, player);
    }
  }

  private void processPlayerVertices(Map map, Player player) {
    for (Vertex vertex : map.getVertices()) {
      double totalValue = vertex.getValue().getProductionValue() * player.getTactic().getProductionWeight()
          + vertex.getValue().getResourceDiversityValue() * player.getTactic().getResourceDiversityWeight()
          + vertex.getValue().getNumberDiversityValue() * player.getTactic().getNumberDiversityWeight()
          + vertex.getValue().getScarcityValue() * player.getTactic().getScarcityWeight();
      switch (player.getTactic()) {
        case BALANCED:
          vertex.getValue().setBalancedValue(MathUtil.round2(totalValue));
          break;
        case PRODUCTION_FOCUSED:
          vertex.getValue().setProductionFocusedValue(MathUtil.round2(totalValue));
          break;
        case SCARCITY_FOCUSED:
          vertex.getValue().setScarcityFocusedValue(MathUtil.round2(totalValue));
          break;
      }
    }
  }

  private double simulateSettlement(Map map, Player player, Vertex vertex) {
    player.getSettlements().add(vertex.getId());
    double score = evaluatePlayer(map, player, false);
    player.getSettlements().remove(player.getSettlements().size()-1);
    return score;
  }

  public java.util.Map<Vertex, Double> getHeuristicsByPlayer(Map map, Player player) {
    java.util.Map<Vertex, Double> heuristics = new HashMap<>();
    for (Vertex vertex : map.getVertices()) {
      if (vertex.isSettled() || player.getPlannedSettments().contains(vertex.getId())) 
        heuristics.put(vertex, 0.0);
      else {
        double score = simulateSettlement(map, player, vertex);
        heuristics.put(vertex, score);
      }
    }
    return heuristics;
  }

  public double evaluatePlayer(Map map, Player player, boolean isSettled) {
    List<Field> fields = new ArrayList<>();
    for (Integer vertexId : player.getSettlements()) {
      fields.addAll(vertexService.getFieldsByVertex(map, vertexId));
    }
    double productionValue = fieldService.calculateProduction(fields);
    double resourceDiversityValue = fieldService.calculateResourceDiversity(fields);
    double numberDiversityValue = fieldService.calculateNumberDiversity(fields);
    double scarcityValue = fieldService.calculateScarcity(map, fields);
    Heuristic heuristic = new Heuristic(productionValue, resourceDiversityValue, numberDiversityValue, scarcityValue);
    MathUtil.roundHeuristic(heuristic, scalingContext, player.getSettlements().size());
    if (isSettled)
      player.getScore().setValues(heuristic.getProductionValue(), heuristic.getResourceDiversityValue(), heuristic.getNumberDiversityValue(), heuristic.getScarcityValue());
    return player.getScore().calculateTotal(heuristic);
  }

  public Vertex findRoute(Map map, Vertex vertex, Player player) {
    double bestHeuristic = 0;
    Vertex bestNeighbour = null;
    Vertex bestPlanned = null;
    for (Integer neighbourId : vertex.getRoadFlags().keySet()) {
      Vertex neighbour = vertexService.getVertex(map, neighbourId);
      java.util.Map<Vertex, Double> result = getBestNeighbour(map, neighbour, player);
      for (java.util.Map.Entry<Vertex, Double> entry : result.entrySet()) {
        if (entry.getValue() > bestHeuristic) {
          bestHeuristic = entry.getValue();
          bestNeighbour = neighbour;
          bestPlanned = entry.getKey();
        }
      }
    }
    if (bestPlanned != null)
      player.getPlannedSettments().add(bestPlanned.getId());
    return bestNeighbour;
  }

  private java.util.Map<Vertex, Double> getBestNeighbour(Map map, Vertex vertex, Player player) {
    double bestHeuristic = 0;
    java.util.Map<Vertex, Double> bestEntry = new HashMap<>();
    for (Integer neighbourId : vertex.getRoadFlags().keySet()) {
      Vertex neighbour = vertexService.getVertex(map, neighbourId);
      if (neighbour.isSettled() || player.getPlannedSettments().contains(neighbour.getId())) 
        continue;
      double heuristic = getHeuristicsByPlayer(map, player).get(neighbour);
      if (heuristic > bestHeuristic) {
        bestHeuristic = heuristic;
        bestEntry.put(neighbour, heuristic);
      }
    }
    return bestEntry;
  }

  public void assignHeatmapRatings(Map map) {
    List<Vertex> vertices = map.getVertices();
    if (vertices == null || vertices.isEmpty()) {
      return;
    }
    double minTotal = Double.POSITIVE_INFINITY;
    double maxTotal = Double.NEGATIVE_INFINITY;
    for (Vertex v : vertices) {
      double t = v.getValue().getOverallValue();
      minTotal = Math.min(minTotal, t);
      maxTotal = Math.max(maxTotal, t);
    }
    double[] heatmapCaps = ConfigLoader.loadHeatmapRatingCaps();
    for (Vertex v : vertices) {
      double overallValue = v.getValue().getOverallValue();
      v.setHeatmapRating(HeatmapRating.fromBoardTotals(overallValue, minTotal, maxTotal, heatmapCaps));
    }
  }
}
