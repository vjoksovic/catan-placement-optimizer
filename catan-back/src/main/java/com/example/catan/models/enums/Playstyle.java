package com.example.catan.models.enums;

import com.example.catan.utils.ConfigLoader;

import lombok.Getter;

import java.util.Map;
import java.util.Set;

@Getter
public enum Playstyle {
  BALANCED,
  PRODUCTION_FOCUSED,
  SCARCITY_FOCUSED;

  private static final Set<String> WEIGHT_KEYS =
      Set.of("productionWeight", "resourceDiversityWeight", "numberDiversityWeight", "scarcityWeight");

  private double productionWeight;
  private double resourceDiversityWeight;
  private double numberDiversityWeight;
  private double scarcityWeight;

  static {
    applyConfiguredWeights();
  }

  private static void applyConfiguredWeights() {
    Map<String, Map<String, Double>> cfg = ConfigLoader.loadPlaystyleWeights();
    for (Playstyle p : values()) {
      Map<String, Double> w = cfg.get(p.name());
      if (w == null || w.isEmpty()) {
        throw new IllegalStateException("heuristic.json: missing playstyleWeights." + p.name());
      }
      for (String key : WEIGHT_KEYS) {
        if (!w.containsKey(key)) {
          throw new IllegalStateException("heuristic.json: playstyleWeights." + p.name() + " missing " + key);
        }
      }
      p.productionWeight = w.get("productionWeight");
      p.resourceDiversityWeight = w.get("resourceDiversityWeight");
      p.numberDiversityWeight = w.get("numberDiversityWeight");
      p.scarcityWeight = w.get("scarcityWeight");
    }
  }

}
