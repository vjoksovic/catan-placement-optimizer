package com.example.catan.services;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.catan.models.enums.HeatmapRating;
import com.example.catan.models.enums.Resource;
import com.example.catan.models.map.Field;
import com.example.catan.models.map.Map;
import com.example.catan.models.map.Player;
import com.example.catan.models.map.Vertex;
import com.example.catan.models.values.Heuristic;
import com.example.catan.utils.ConfigLoader;

import org.springframework.stereotype.Service;

@Service
public class HeuristicService {

  private final MapService mapService;
  private final java.util.Map<String, Double> scarcityMultipliers;

  public HeuristicService(MapService mapService) {
    this.mapService = mapService;
    this.scarcityMultipliers = ConfigLoader.loadScarcityResourceMultipliers();
  }
  
  public void calculateHeuristic(Map map) {
    for (Vertex vertex : map.getVertices()) {
      calculateVertexHeuristic(map, vertex);
    }
    assignHeatmapRatings(map);
  }

  public void calculateIndividualHeuristic(Map map) {
    for (Player player : map.getPlayers()) {
      calculateIndividualVertices(map, player);
    }
  }

  private void calculateIndividualVertices(Map map, Player player) {
    for (Vertex vertex : map.getVertices()) {
      double totalValue = vertex.getValue().getProductionValue() * player.getPlaystyle().getProductionWeight()
          + vertex.getValue().getResourceDiversityValue() * player.getPlaystyle().getResourceDiversityWeight()
          + vertex.getValue().getNumberDiversityValue() * player.getPlaystyle().getNumberDiversityWeight()
          + vertex.getValue().getScarcityValue() * player.getPlaystyle().getScarcityWeight();
      vertex.getValue().setTotalValue(round1(totalValue));
    }
  }

  private void calculateVertexHeuristic(Map map, Vertex vertex) {
    List<Field> fields = mapService.getFields(map, vertex.getId());
    calculateProduction(vertex, fields);
    calculateResourceDiversity(vertex, fields);
    calculateNumberDiversity(vertex, fields);
    calculateScarcity(map, vertex, fields);
    roundHeuristicToOneDecimal(vertex);
  }

  /** Round each component and total to one decimal (half-up). */
  private static void roundHeuristicToOneDecimal(Vertex vertex) {
    Heuristic h = vertex.getValue();
    double p = round1(h.getProductionValue());
    double r = round1(h.getResourceDiversityValue());
    double n = round1(h.getNumberDiversityValue());
    double s = round1(h.getScarcityValue());
    h.setProductionValue(p);
    h.setResourceDiversityValue(r);
    h.setNumberDiversityValue(n);
    h.setScarcityValue(s);
    h.setTotalValue(round1(p + r + n + s));
  }

  private static double round1(double x) {
    return Math.round(x * 10.0) / 10.0;
  }

  private void assignHeatmapRatings(Map map) {
    List<Vertex> vertices = map.getVertices();
    if (vertices == null || vertices.isEmpty()) {
      return;
    }
    double minTotal = Double.POSITIVE_INFINITY;
    double maxTotal = Double.NEGATIVE_INFINITY;
    for (Vertex v : vertices) {
      double t = v.getValue().getTotalValue();
      minTotal = Math.min(minTotal, t);
      maxTotal = Math.max(maxTotal, t);
    }
    double[] heatmapCaps = ConfigLoader.loadHeatmapRatingCaps();
    for (Vertex v : vertices) {
      double totalValue = v.getValue().getTotalValue();
      v.setHeatmapRating(HeatmapRating.fromBoardTotals(totalValue, minTotal, maxTotal, heatmapCaps));
    }
  }

  private void calculateProduction(Vertex vertex, List<Field> fields) {
    double productionValue = 0;
    for (Field field : fields) {
      productionValue += calulateFieldProductionValue(field);
    }
    vertex.getValue().setProductionValue(productionValue);
  }

  private double calulateFieldProductionValue(Field field) {
    return field.getProductionValue() * field.getResource().getWeight();
  }

  private void calculateResourceDiversity(Vertex vertex, List<Field> fields) {
    Set<Resource> resources = new HashSet<>();
    for (Field field : fields) {
      resources.add(field.getResource());
    }
    vertex.getValue().setResourceDiversityValue(resources.size());
  }

  private void calculateNumberDiversity(Vertex vertex, List<Field> fields) {
    Set<Integer> numbers = new HashSet<>();
    double numberDiversityValue = 0;
    for (Field field : fields) {
      if (numbers.contains(field.getFieldNumber())) {
        double penalty = 20 * calulateFieldProductionValue(field) / 100;
        numberDiversityValue -= penalty;
      } else {
        numbers.add(field.getFieldNumber());
        numberDiversityValue++;
      }
    }
    vertex.getValue().setNumberDiversityValue(numberDiversityValue);
  }

  private void calculateScarcity(Map map, Vertex vertex, List<Field> fields) {
    double scarcityValue = 0;
    double maxProduction = mapService.calculateMaxProduction(map);
    for (Field field : fields) {
      double points = maxProduction / map.getProduction().get(field.getResource()) * field.getProductionValue();
      Double mult = scarcityMultipliers.get(field.getResource().name());
      if (mult != null) {
        points *= mult;
      }
      scarcityValue += points;
    }
    vertex.getValue().setScarcityValue(scarcityValue / fields.size());
  }
}
