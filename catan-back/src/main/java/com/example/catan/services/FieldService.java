package com.example.catan.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.catan.interfaces.HeuristicInterface;
import com.example.catan.models.enums.Resource;
import com.example.catan.models.map.Field;
import com.example.catan.models.map.Map;
import com.example.catan.models.map.Vertex;
import com.example.catan.utils.ConfigLoader;
import com.example.catan.utils.MathUtil;

@Service
public class FieldService implements HeuristicInterface {

  private final java.util.Map<String, Double> scarcityMultipliers;
  private final java.util.Map<String, Double> numberMultipliers;
  private final MathUtil.HeuristicScalingContext scalingContext;
  private final VertexService vertexService;

  public FieldService(VertexService vertexService) {
    this.vertexService = vertexService;
    this.scarcityMultipliers = ConfigLoader.loadScarcityResourceMultipliers();
    this.numberMultipliers = ConfigLoader.loadNumberMultipliers();
    java.util.Map<String, Double> maxValues = ConfigLoader.loadHeuristicScalingMaxValues();
    java.util.Map<String, Double> targetShares = ConfigLoader.loadHeuristicScalingTargetShares();
    this.scalingContext = MathUtil.buildHeuristicScalingContext(maxValues, targetShares);
  }

  public double calculateProduction(List<Field> fields) {
    double productionValue = 0;
    for (Field field : fields) {
      productionValue += calulateFieldProductionValue(field);
    }
    return productionValue;
  }

  private double calulateFieldProductionValue(Field field) {
    return field.getProductionValue() * field.getResource().getWeight();
  }

  public double calculateResourceDiversity(List<Field> fields) {
    Set<Resource> resources = new HashSet<>();
    double resourceDiversityValue = 0;
    for (Field field : fields) {
      if (field.getResource() == Resource.DESERT) continue;
      if (resources.add(field.getResource())) 
        resourceDiversityValue += field.getResource().getWeight();
    }
    return resourceDiversityValue;
  }

  public double calculateNumberDiversity(List<Field> fields) {
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

  public double calculateScarcity(Map map, List<Field> fields) {
    double scarcityValue = 0;
    double maxProduction = getMaxProduction(map);
    for (Field field : fields) {
      double minProduction = getMinProduction(map, field);
      if (minProduction > 0.0) {
        scarcityValue = MathUtil.round2(maxProduction / minProduction) * scarcityMultipliers.get(field.getResource().name());
        return scarcityValue;
      }
    }
    return scarcityValue;
  }

  private double getMaxProduction(Map map) {
    int maxProduction = 0;
    for (java.util.Map.Entry<Resource, Integer> entry : map.getProduction().entrySet()) {
      maxProduction = Math.max(maxProduction, entry.getValue());
    }
    return maxProduction;
  }

  private double getMinProduction(Map map, Field field) {
    for (java.util.Map.Entry<Resource, Integer> entry : map.getProduction().entrySet()) {
      if (entry.getKey() == field.getResource() && entry.getKey() != Resource.DESERT && entry.getValue() < 10.0)
        return entry.getValue();
    }
    return 0.0;
  }

  public void calculateVertexHeuristic(Map map, Vertex vertex) {
    List<Field> fields = vertexService.getFieldsByVertex(map, vertex.getId());
    vertex.getValue().setProductionValue(calculateProduction(fields));
    vertex.getValue().setResourceDiversityValue(calculateResourceDiversity(fields));
    vertex.getValue().setNumberDiversityValue(calculateNumberDiversity(fields));
    vertex.getValue().setScarcityValue(calculateScarcity(map, fields));
    MathUtil.roundHeuristic(vertex.getValue(), scalingContext, 1);
  }
}
