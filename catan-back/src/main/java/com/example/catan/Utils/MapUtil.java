package com.example.catan.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.example.catan.models.Resource;

public class MapUtil {
  
  public static HashMap<Integer, List<Integer>> forbiddenNumbers() {
    HashMap<Integer, List<Integer>> forbiddenNumbers = new HashMap<>();
    forbiddenNumbers.put(0, new ArrayList<>());
    forbiddenNumbers.put(1, new ArrayList<>());
    forbiddenNumbers.put(2, new ArrayList<>());
    forbiddenNumbers.put(3, new ArrayList<>());
    forbiddenNumbers.put(4, new ArrayList<>());
    forbiddenNumbers.put(5, new ArrayList<>());
    forbiddenNumbers.put(6, new ArrayList<>());
    forbiddenNumbers.put(7, new ArrayList<>());
    forbiddenNumbers.put(8, new ArrayList<>());
    forbiddenNumbers.put(9, new ArrayList<>());
    forbiddenNumbers.put(10, new ArrayList<>());
    forbiddenNumbers.put(11, new ArrayList<>());
    forbiddenNumbers.put(12, new ArrayList<>());
    forbiddenNumbers.put(13, new ArrayList<>());
    forbiddenNumbers.put(14, new ArrayList<>());
    forbiddenNumbers.put(15, new ArrayList<>());
    forbiddenNumbers.put(16, new ArrayList<>());
    forbiddenNumbers.put(17, new ArrayList<>());
    forbiddenNumbers.put(18, new ArrayList<>());
    return forbiddenNumbers;
  }

  public static HashMap<Integer, List<String>> forbiddenResources() {
    HashMap<Integer, List<String>> forbiddenResources = new HashMap<>();
    forbiddenResources.put(0, new ArrayList<>());
    forbiddenResources.put(1, new ArrayList<>());
    forbiddenResources.put(2, new ArrayList<>());
    forbiddenResources.put(3, new ArrayList<>());
    forbiddenResources.put(4, new ArrayList<>());
    forbiddenResources.put(5, new ArrayList<>());
    forbiddenResources.put(6, new ArrayList<>());
    forbiddenResources.put(7, new ArrayList<>());
    forbiddenResources.put(8, new ArrayList<>());
    forbiddenResources.put(9, new ArrayList<>());
    forbiddenResources.put(10, new ArrayList<>());
    forbiddenResources.put(11, new ArrayList<>());
    forbiddenResources.put(12, new ArrayList<>());
    forbiddenResources.put(13, new ArrayList<>());
    forbiddenResources.put(14, new ArrayList<>());
    forbiddenResources.put(15, new ArrayList<>());
    forbiddenResources.put(16, new ArrayList<>());
    forbiddenResources.put(17, new ArrayList<>());
    forbiddenResources.put(18, new ArrayList<>());
    return forbiddenResources;
  }

  private static int randomBetween(int min, int max) {
    if (min > max) {
      throw new IllegalArgumentException("min must be <= max");
    }
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }

  public static Resource randomResource() {
    return Resource.values()[randomBetween(0, Resource.values().length - 1)];
  }

  public static int randomNumber() {
    int[] numbers = {0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    return numbers[randomBetween(0, numbers.length - 1)];
  }
}
