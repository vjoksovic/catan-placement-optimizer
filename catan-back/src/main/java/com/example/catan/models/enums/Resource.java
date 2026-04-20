package com.example.catan.models.enums;

import com.example.catan.utils.ConfigLoader;

import lombok.Getter;

import java.util.Map;

@Getter
public enum Resource {
  
  WOOD(0),
  BRICK(0),
  SHEEP(0),
  WHEAT(0),
  ORE(0),
  DESERT(0);

  private double weight;

  Resource(double weight) {
    this.weight = weight;
  }

  static {
    applyConfiguredWeights();
  }

  private static void applyConfiguredWeights() {
    Map<String, Double> configuredWeights = ConfigLoader.loadResourceWeights();
    for (Resource resource : values()) {
      Double configuredWeight = configuredWeights.get(resource.name());
      if (configuredWeight != null) {
        resource.weight = configuredWeight;
      }
    }
  }

}
