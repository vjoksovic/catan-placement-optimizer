package com.example.catan.services;

import com.example.catan.models.enums.Playstyle;
import com.example.catan.models.enums.Resource;
import com.example.catan.models.map.Field;
import com.example.catan.models.map.Map;
import com.example.catan.models.map.Vertex;
import com.example.catan.utils.ConfigLoader;
import com.example.catan.utils.MapGeneratorUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GeneratorService {

  private Map currentMap;

  public GeneratorService() {
    this.currentMap = null;
  }

  public synchronized Map getCurrentMap() {
    if (currentMap == null) {
      throw new IllegalStateException("Map is not generated");
    }
    return currentMap;
  }

  public synchronized Map generateNew(List<Playstyle> playstyles) {
    currentMap = generateInternal(playstyles);
    return currentMap;
  }

  private Map generateInternal(List<Playstyle> playstyles) {
    final int maxAttempts = 200;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      Map map = new Map(playstyles);
      HashMap<String, Integer> resources = ConfigLoader.loadResourceMap();
      HashMap<Integer, Integer> numbers = ConfigLoader.loadNumberMap();
      HashMap<Integer, Integer> productionByNumber = ConfigLoader.loadProductionMap();
      HashMap<Integer, List<Integer>> forbiddenNumbers = MapGeneratorUtil.forbiddenNumbers();
      HashMap<Integer, List<String>> forbiddenResources = MapGeneratorUtil.forbiddenResources();
      try {
        for (int i = 0; i <= 18; i++) {
          Field current = generateField(map, i, resources, numbers, productionByNumber, forbiddenNumbers, forbiddenResources);
          updateForbidden(current, forbiddenNumbers, forbiddenResources);
        }
        return map;
      } catch (IllegalStateException ignored) {
        // Dead-end for this random run; retry from scratch.
      }
    }
    throw new IllegalStateException("Failed to generate valid map after retries");
  }

  private boolean isForbiddenResource(int fieldId, String resource, HashMap<Integer, List<String>> forbiddenResources) {
    return forbiddenResources.get(fieldId).contains(resource);
  }

  private boolean isForbiddenNumber(int fieldId, int number, HashMap<Integer, List<Integer>> forbiddenNumbers) {
    return forbiddenNumbers.get(fieldId).contains(number);
  }

  private Field generateField(
      Map map,
      int fieldId,
      HashMap<String, Integer> resources,
      HashMap<Integer, Integer> numbers,
      HashMap<Integer, Integer> productionByNumber,
      HashMap<Integer, List<Integer>> forbiddenNumbers,
      HashMap<Integer, List<String>> forbiddenResources
  ) {
    Field current = map.getFields().get(fieldId);
    generateNumber(map, fieldId, numbers, forbiddenNumbers);
    if (current.getFieldNumber() == 0) 
      generateResource(map, fieldId, true, resources, forbiddenResources);
    else
      generateResource(map, fieldId, false, resources, forbiddenResources);
    assignProduction(map, fieldId, productionByNumber);
    return current;
  }

  private void generateNumber(
      Map map,
      int fieldId,
      HashMap<Integer, Integer> numbers,
      HashMap<Integer, List<Integer>> forbiddenNumbers
  ) {
    int number = randomAvailableNumber(fieldId, numbers, forbiddenNumbers);
    map.getFields().get(fieldId).setFieldNumber(number);
    numbers.put(number, numbers.get(number) - 1);
  }

  private void generateResource(
      Map map,
      int fieldId,
      boolean isDesert,
      HashMap<String, Integer> resources,
      HashMap<Integer, List<String>> forbiddenResources
  ) {
    if (isDesert) {
      map.getFields().get(fieldId).setResource(Resource.DESERT);
      resources.put("DESERT", resources.get("DESERT") - 1);
      return;
    }
    Resource resource = randomAvailableResource(fieldId, false, resources, forbiddenResources);
    map.getFields().get(fieldId).setResource(resource);
    String key = resource.toString();
    resources.put(key, resources.get(key) - 1);
  }

  private void assignProduction(Map map, int fieldId, HashMap<Integer, Integer> productionByNumber) {
    Field f = map.getFields().get(fieldId);
    int number = f.getFieldNumber();
    int productionValue = productionByNumber.getOrDefault(number, 0);
    if (f.getResource() == Resource.DESERT) {
      productionValue = 0;
    }
    f.setProductionValue(productionValue);
    Resource resource = f.getResource();
    map.getProduction().put(resource, map.getProduction().getOrDefault(resource, 0) + productionValue);
  }

  private int randomAvailableNumber(
      int fieldId,
      HashMap<Integer, Integer> numbers,
      HashMap<Integer, List<Integer>> forbiddenNumbers
  ) {
    List<Integer> available = new ArrayList<>();
    for (var entry : numbers.entrySet()) {
      if (entry.getValue() != null && entry.getValue() > 0 && !isForbiddenNumber(fieldId, entry.getKey(), forbiddenNumbers)) {
        available.add(entry.getKey());
      }
    }
    if (available.isEmpty()) {
      throw new IllegalStateException("No available numbers for field " + fieldId);
    }
    return available.get(ThreadLocalRandom.current().nextInt(available.size()));
  }

  private Resource randomAvailableResource(
      int fieldId,
      boolean includeDesert,
      HashMap<String, Integer> resources,
      HashMap<Integer, List<String>> forbiddenResources
  ) {
    List<Resource> available = new ArrayList<>();
    for (Resource resource : Resource.values()) {
      if (!includeDesert && resource == Resource.DESERT) {
        continue;
      }
      if (resources.getOrDefault(resource.toString(), 0) > 0
          && !isForbiddenResource(fieldId, resource.toString(), forbiddenResources)) {
        available.add(resource);
      }
    }
    if (available.isEmpty()) {
      throw new IllegalStateException("No available resources for field " + fieldId);
    }
    return available.get(ThreadLocalRandom.current().nextInt(available.size()));
  }

  private void updateForbidden(
      Field current,
      HashMap<Integer, List<Integer>> forbiddenNumbers,
      HashMap<Integer, List<String>> forbiddenResources
  ) {
    for (int neighbour : current.getNeighbours()) {
      
      if (current.getFieldNumber() == 6 || current.getFieldNumber() == 8) {
        forbiddenNumbers.get(neighbour).add(6);
        forbiddenNumbers.get(neighbour).add(8);
      }
      else if (current.getFieldNumber() == 2 || current.getFieldNumber() == 12) {
        forbiddenNumbers.get(neighbour).add(2);
        forbiddenNumbers.get(neighbour).add(12);
      }
      else
        forbiddenNumbers.get(neighbour).add(current.getFieldNumber());
      forbiddenResources.get(neighbour).add(current.getResource().toString());
    }
  }

 
}
