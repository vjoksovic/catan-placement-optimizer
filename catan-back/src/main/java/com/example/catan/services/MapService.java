package com.example.catan.services;

import com.example.catan.models.Field;
import com.example.catan.models.Map;
import com.example.catan.models.enums.Resource;
import com.example.catan.utils.ConfigLoader;
import com.example.catan.utils.MapGeneratorUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MapService {

  private Map map;
  private HashMap<String, Integer> resources;
  private HashMap<Integer, Integer> numbers;
  private HashMap<Integer, Integer> productionByNumber;
  private HashMap<Integer, List<Integer>> forbiddenNumbers;
  private HashMap<Integer, List<String>> forbiddenResources;

  public MapService() {
    this.map = new Map();
    this.resources = ConfigLoader.loadResourceMap();
    this.numbers = ConfigLoader.loadNumberMap();
    this.productionByNumber = ConfigLoader.loadProductionMap();
    this.forbiddenNumbers = MapGeneratorUtil.forbiddenNumbers();
    this.forbiddenResources = MapGeneratorUtil.forbiddenResources();
  }

  private void initialize() {
    this.map = new Map();
    this.resources = ConfigLoader.loadResourceMap();
    this.numbers = ConfigLoader.loadNumberMap();
    this.productionByNumber = ConfigLoader.loadProductionMap();
    this.forbiddenNumbers = MapGeneratorUtil.forbiddenNumbers();
    this.forbiddenResources = MapGeneratorUtil.forbiddenResources();
  }

  public Map generateNew() {
    final int maxAttempts = 200;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      initialize();
      try {
        for (int i = 0; i <= 18; i++) {
          Field current = generateField(i);
          updateForbidden(current);
        }
        return map;
      } catch (IllegalStateException ignored) {
        // Dead-end for this random run; retry from scratch.
      }
    }
    throw new IllegalStateException("Failed to generate valid map after retries");
  }

  private boolean isForbiddenResource(int fieldId, String resource) {
    return forbiddenResources.get(fieldId).contains(resource);
  }

  private boolean isForbiddenNumber(int fieldId, int number) {
    return forbiddenNumbers.get(fieldId).contains(number);
  }

  private Field generateField(int fieldId) {
    Field current = map.getFields().get(fieldId);
    generateNumber(fieldId);
    if (current.getFieldNumber() == 0) 
      generateResource(fieldId, true);
    else
      generateResource(fieldId, false);
    assignProduction(fieldId);
    return current;
  }

  private void generateNumber(int fieldId) {
    int number = randomAvailableNumber(fieldId);
    map.getFields().get(fieldId).setFieldNumber(number);
    numbers.put(number, numbers.get(number) - 1);
  }

  private void generateResource(int fieldId, boolean isDesert) {
    if (isDesert) {
      map.getFields().get(fieldId).setResource(Resource.DESERT);
      resources.put("DESERT", resources.get("DESERT") - 1);
      return;
    }
    Resource resource = randomAvailableResource(fieldId, false);
    map.getFields().get(fieldId).setResource(resource);
    String key = resource.toString();
    resources.put(key, resources.get(key) - 1);
  }

  private void assignProduction(int fieldId) {
    Field f = map.getFields().get(fieldId);
    int number = f.getFieldNumber();
    int productionValue = productionByNumber.getOrDefault(number, 0);
    if (f.getResource() == Resource.DESERT) {
      productionValue = 0;
    }
    f.setProductionValue(productionValue);
  }

  private int randomAvailableNumber(int fieldId) {
    List<Integer> available = new ArrayList<>();
    for (var entry : numbers.entrySet()) {
      if (entry.getValue() != null && entry.getValue() > 0 && !isForbiddenNumber(fieldId, entry.getKey())) {
        available.add(entry.getKey());
      }
    }
    if (available.isEmpty()) {
      throw new IllegalStateException("No available numbers for field " + fieldId);
    }
    return available.get(ThreadLocalRandom.current().nextInt(available.size()));
  }

  private Resource randomAvailableResource(int fieldId, boolean includeDesert) {
    List<Resource> available = new ArrayList<>();
    for (Resource resource : Resource.values()) {
      if (!includeDesert && resource == Resource.DESERT) {
        continue;
      }
      if (resources.getOrDefault(resource.toString(), 0) > 0
          && !isForbiddenResource(fieldId, resource.toString())) {
        available.add(resource);
      }
    }
    if (available.isEmpty()) {
      throw new IllegalStateException("No available resources for field " + fieldId);
    }
    return available.get(ThreadLocalRandom.current().nextInt(available.size()));
  }

  private void updateForbidden(Field current) {
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
