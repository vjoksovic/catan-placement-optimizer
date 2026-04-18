package com.example.catan.services;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.catan.models.enums.Resource;
import com.example.catan.models.map.Field;
import com.example.catan.models.map.Map;
import com.example.catan.models.map.Player;
import com.example.catan.models.map.Vertex;
import com.example.catan.models.values.Heuristic;
import com.example.catan.utils.ConfigLoader;
import com.example.catan.utils.MathUtil;

import org.springframework.stereotype.Service;

@Service
public class HeuristicService {

  private final MapService mapService;
  private final VertexService vertexService;
  private final java.util.Map<String, Double> scarcityMultipliers;
  private final java.util.Map<String, Double> numberMultipliers;
  private final MathUtil.HeuristicScalingContext scalingContext;

  public HeuristicService(MapService mapService, VertexService vertexService) {
    this.mapService = mapService;
    this.vertexService = vertexService;
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
    mapService.processPlayerHeuristics(map);
  }

  public void evaluatePlayer(Map map, Player player) {
    List<Field> fields = new ArrayList<>();
    for (Integer vertexId : player.getSettlements()) {
      fields.addAll(vertexService.getFieldsByVertex(map, vertexId));
    }
    double productionValue = calculateProduction(fields);
    double resourceDiversityValue = calculateResourceDiversity(fields);
    double numberDiversityValue = calculateNumberDiversity(fields);
    double scarcityValue = calculateScarcity(map, fields);
    Heuristic heuristic = new Heuristic(productionValue, resourceDiversityValue, numberDiversityValue, scarcityValue);
    MathUtil.roundHeuristic(heuristic, scalingContext, player.getSettlements().size());
    player.getScore().setValues(heuristic.getProductionValue(), heuristic.getResourceDiversityValue(), heuristic.getNumberDiversityValue(), heuristic.getScarcityValue());
  }

  private void calculateVertexHeuristic(Map map, Vertex vertex) {
    List<Field> fields = vertexService.getFieldsByVertex(map, vertex.getId());
    vertex.getValue().setProductionValue(calculateProduction(fields));
    vertex.getValue().setResourceDiversityValue(calculateResourceDiversity(fields));
    vertex.getValue().setNumberDiversityValue(calculateNumberDiversity(fields));
    vertex.getValue().setScarcityValue(calculateScarcity(map, fields));
    MathUtil.roundHeuristic(vertex.getValue(), scalingContext, 1);
  }

  private double calculateProduction(List<Field> fields) {
    double productionValue = 0;
    for (Field field : fields) {
      productionValue += calulateFieldProductionValue(field);
    }
    return productionValue;
  }

  private double calulateFieldProductionValue(Field field) {
    return field.getProductionValue() * field.getResource().getWeight();
  }

  private double calculateResourceDiversity(List<Field> fields) {
    Set<Resource> resources = new HashSet<>();
    double resourceDiversityValue = 0;
    for (Field field : fields) {
      if (field.getResource() == Resource.DESERT) continue;
      if (resources.add(field.getResource())) 
        resourceDiversityValue += field.getResource().getWeight();
    }
    return resourceDiversityValue;
  }

  private double calculateNumberDiversity(List<Field> fields) {
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
    return numberDiversityValue;
  }

  private double calculateScarcity(Map map, List<Field> fields) {
    double scarcityValue = 0;
    double maxProduction = mapService.getMaxProduction(map);
    for (Field field : fields) {
      double minProduction = mapService.getMinProduction(map, field);
      if (minProduction > 0.0) {
        scarcityValue = MathUtil.round2(maxProduction / minProduction) * scarcityMultipliers.get(field.getResource().name());
        return scarcityValue;
      }
    }
    return scarcityValue;
  }
}
