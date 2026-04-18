package com.example.catan.services;

import java.util.HashMap;
import java.util.List;

import com.example.catan.models.map.Map;
import com.example.catan.models.map.Player;
import com.example.catan.models.map.Vertex;
import com.example.catan.utils.ConfigLoader;
import com.example.catan.utils.MathUtil;

import com.example.catan.models.map.Field;
import com.example.catan.models.enums.HeatmapRating;
import com.example.catan.models.enums.Resource;
import org.springframework.stereotype.Service;

@Service
public class MapService {

  private final VertexService vertexService;
  
  public MapService(VertexService vertexService) {
    this.vertexService = vertexService;
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

  public double getMaxProduction(Map map) {
    int maxProduction = 0;
    for (java.util.Map.Entry<Resource, Integer> entry : map.getProduction().entrySet()) {
      maxProduction = Math.max(maxProduction, entry.getValue());
    }
    return maxProduction;
  }

  public double getMinProduction(Map map, Field field) {
    for (java.util.Map.Entry<Resource, Integer> entry : map.getProduction().entrySet()) {
      if (entry.getKey() == field.getResource() && entry.getKey() != Resource.DESERT && entry.getValue() < 10.0)
        return entry.getValue();
    }
    return 0.0;
  }

  public java.util.Map<Vertex, Double> getHeuristicsByPlayer(Map map, Player player) {
    java.util.Map<Vertex, Double> heuristics = new HashMap<>();
    for (Vertex vertex : map.getVertices()) {
      if (vertex.isSettled() || player.getPlannedSettments().contains(vertex.getId())) 
        heuristics.put(vertex, 0.0);
      else 
        heuristics.put(vertex, vertexService.getHeuristicByPlayer(vertex, player));
    }
    return heuristics;
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
      double heuristic = vertexService.getHeuristicByPlayer(neighbour, player);
      if (heuristic > bestHeuristic) 
        bestHeuristic = heuristic;
        bestEntry.put(neighbour, heuristic);
    }
    return bestEntry;
  }

  public Vertex findSettlement(Map map, Player player) {
    java.util.Map<Vertex, Double> heuristics = getHeuristicsByPlayer(map, player);
    double bestHeuristic = 0;
    Vertex bestSettlement = null;
    for (java.util.Map.Entry<Vertex, Double> entry : heuristics.entrySet()) {
      if (entry.getValue() > bestHeuristic) {
        bestHeuristic = entry.getValue();
        bestSettlement = entry.getKey();
      }
    }
    return bestSettlement;
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
