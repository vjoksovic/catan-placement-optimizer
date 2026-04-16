package com.example.catan.services;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.catan.models.enums.Resource;
import com.example.catan.models.map.Field;
import com.example.catan.models.map.Map;
import com.example.catan.models.map.Player;
import com.example.catan.models.map.Vertex;
import com.example.catan.utils.ConfigLoader;
import com.example.catan.utils.MathUtil;

import org.springframework.stereotype.Service;

@Service
public class HeuristicService {

  private final MapService mapService;
  private final java.util.Map<String, Double> scarcityMultipliers;
  private final java.util.Map<String, Double> numberMultipliers;
  private final MathUtil.HeuristicScalingContext scalingContext;

  public HeuristicService(MapService mapService) {
    this.mapService = mapService;
    this.scarcityMultipliers = ConfigLoader.loadScarcityResourceMultipliers();
    this.numberMultipliers = ConfigLoader.loadNumberMultipliers();
    java.util.Map<String, Double> maxValues = ConfigLoader.loadHeuristicScalingMaxValues();
    java.util.Map<String, Double> targetShares = ConfigLoader.loadHeuristicScalingTargetShares();
    this.scalingContext = MathUtil.buildHeuristicScalingContext(maxValues, targetShares);
  }
  
  public void calculateHeuristic(Map map) {
    for (Vertex vertex : map.getVertices()) {
      calculateVertexHeuristic(map, vertex);
    }
    mapService.assignHeatmapRatings(map);
  }

  public void processPlayerHeuristics(Map map) {
    for (Player player : map.getPlayers()) {
      processPlayerVertices(map, player);
    }
  }

  private void processPlayerVertices(Map map, Player player) {
    for (Vertex vertex : map.getVertices()) {
      double totalValue = vertex.getValue().getProductionValue() * player.getPlaystyle().getProductionWeight()
          + vertex.getValue().getResourceDiversityValue() * player.getPlaystyle().getResourceDiversityWeight()
          + vertex.getValue().getNumberDiversityValue() * player.getPlaystyle().getNumberDiversityWeight()
          + vertex.getValue().getScarcityValue() * player.getPlaystyle().getScarcityWeight();
      vertex.getValue().setOverallValue(MathUtil.round1(totalValue));
    }
  }

  private void calculateVertexHeuristic(Map map, Vertex vertex) {
    List<Field> fields = mapService.getFields(map, vertex.getId());
    calculateProduction(vertex, fields);
    calculateResourceDiversity(vertex, fields);
    calculateNumberDiversity(vertex, fields);
    calculateScarcity(map, vertex, fields);
    MathUtil.roundHeuristicToOneDecimal(vertex.getValue(), scalingContext);
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
    double resourceDiversityValue = 0;
    for (Field field : fields) {
      if (field.getResource() == Resource.DESERT) continue;
      if (resources.add(field.getResource())) 
        resourceDiversityValue += field.getResource().getWeight();
    }
    vertex.getValue().setResourceDiversityValue(resourceDiversityValue);
  }

  private void calculateNumberDiversity(Vertex vertex, List<Field> fields) {
    Set<Integer> numbers = new HashSet<>();
    double numberDiversityValue = 0;
    for (Field field : fields) {
      if (field.getResource() == Resource.DESERT) continue;
      double numberMultiplier = MathUtil.resolveNumberMultiplier(numberMultipliers, field.getFieldNumber());
      if (numbers.contains(field.getFieldNumber())) {
        double penalty = MathUtil.duplicateNumberPenalty(calulateFieldProductionValue(field), numberMultiplier);
        numberDiversityValue -= penalty;
      } else {
        numbers.add(field.getFieldNumber());
        numberDiversityValue += numberMultiplier;
      }
    }
    vertex.getValue().setNumberDiversityValue(numberDiversityValue);
  }

  private void calculateScarcity(Map map, Vertex vertex, List<Field> fields) {
    double scarcityValue = 0;
    double maxProduction = mapService.getMaxProduction(map);
    for (Field field : fields) {
      int minProduction = mapService.getMinProduction(map, field);
      if (minProduction > 0) {
        scarcityValue = MathUtil.round1(maxProduction / minProduction) * scarcityMultipliers.get(field.getResource().name());
        vertex.getValue().setScarcityValue(scarcityValue);
        return;
      }
    }
  }
}
