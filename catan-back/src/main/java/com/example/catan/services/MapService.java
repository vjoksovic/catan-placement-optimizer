package com.example.catan.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.catan.models.map.Map;
import com.example.catan.models.map.Vertex;
import com.example.catan.utils.ConfigLoader;

import ch.qos.logback.core.joran.sanity.Pair;

import com.example.catan.models.map.Field;
import com.example.catan.models.enums.HeatmapRating;
import com.example.catan.models.enums.Playstyle;
import com.example.catan.models.enums.Resource;
import org.springframework.stereotype.Service;

@Service
public class MapService {
  
  public List<Field> getFields(Map map, int vertexId) {
    List<Field> fields = new ArrayList<>();
    for (int fieldId : map.getVertices().get(vertexId).getFields()) {
      fields.add(map.getFields().get(fieldId));
    }
    return fields;
  }

  public double getMaxProduction(Map map) {
    int maxProduction = 0;
    for (java.util.Map.Entry<Resource, Integer> entry : map.getProduction().entrySet()) {
      maxProduction = Math.max(maxProduction, entry.getValue());
    }
    return maxProduction;
  }

  public int getMinProduction(Map map, Field field) {
    for (java.util.Map.Entry<Resource, Integer> entry : map.getProduction().entrySet()) {
      if (entry.getKey() == field.getResource() && entry.getKey() != Resource.DESERT && entry.getValue() < 10)
        return entry.getValue();
    }
    return 0;
  }

  public Vertex getVertexByHeuristic(Map map, double heuristic, Playstyle playstyle) {
    for (Vertex vertex : map.getVertices()) {
      switch (playstyle) {
        case BALANCED:
          if (vertex.getValue().getBalancedValue() == heuristic) {
            return vertex;
          }
        case PRODUCTION_FOCUSED:
          if (vertex.getValue().getProductionFocusedValue() == heuristic) {
            return vertex;
          }
        case SCARCITY_FOCUSED:
          if (vertex.getValue().getScarcityFocusedValue() == heuristic) {
            return vertex;
          }
      }
    }
    return null;
  }

  public Vertex getVertex(Map map, int vertexId) {
    return map.getVertices().get(vertexId);
  }

  public java.util.Map<Vertex, Double> getHeuristicsByPlaystyle(Map map, Playstyle playstyle) {
    java.util.Map<Vertex, Double> heuristics = new HashMap<>();
    for (Vertex vertex : map.getVertices()) {
      if (vertex.isSettled()) 
        heuristics.put(vertex, 0.0);
      else 
        heuristics.put(vertex, getHeuristicByPlaystyle(vertex, playstyle));
    }
    return heuristics;
  }

  public double getHeuristicByPlaystyle(Vertex vertex, Playstyle playstyle) {
    switch (playstyle) {
      case BALANCED:
        return vertex.getValue().getBalancedValue();
      case PRODUCTION_FOCUSED:
        return vertex.getValue().getProductionFocusedValue();
      case SCARCITY_FOCUSED:
        return vertex.getValue().getScarcityFocusedValue();
      default:
        return 0;
    }
  }

  public Vertex findRoute(Map map, Vertex vertex, Playstyle playstyle) {
    double bestHeuristic = 0;
    Vertex bestNeighbour = null;
    for (Integer neighbourId : vertex.getRoadFlags().keySet()) {
      Vertex neighbour = getVertex(map, neighbourId);
      double heuristic = getBestHeuristic(map, neighbour, playstyle);
      if (heuristic > bestHeuristic) {
        bestHeuristic = heuristic;
        bestNeighbour = neighbour;
      }
    }
    return bestNeighbour;
  }

  private double getBestHeuristic(Map map, Vertex vertex, Playstyle playstyle) {
    double bestHeuristic = 0;
    for (Integer neighbourId : vertex.getRoadFlags().keySet()) {
      Vertex neighbour = getVertex(map, neighbourId);
      if (neighbour.isSettled()) 
        continue;
      double heuristic = getHeuristicByPlaystyle(neighbour, playstyle);
      if (heuristic > bestHeuristic) {
        bestHeuristic = heuristic;
      }
    }
    return bestHeuristic;
  }

  public void setSettled(Map map, Vertex vertex) {
    vertex.setSettled(true);
    for (Integer neighbourId : vertex.getRoadFlags().keySet()) {
      Vertex neighbour = getVertex(map, neighbourId);
      neighbour.setSettled(true);
    }
  }

  public Vertex findSettlement(Map map, Playstyle playstyle) {
    java.util.Map<Vertex, Double> heuristics = getHeuristicsByPlaystyle(map, playstyle);
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
