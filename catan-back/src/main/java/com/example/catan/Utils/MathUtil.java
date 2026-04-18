package com.example.catan.utils;

import com.example.catan.models.enums.Tactic;
import com.example.catan.models.values.Heuristic;

public final class MathUtil {

  public static final String PRODUCTION_KEY = "production";
  public static final String RESOURCE_DIVERSITY_KEY = "resourceDiversity";
  public static final String NUMBER_DIVERSITY_KEY = "numberDiversity";
  public static final String SCARCITY_KEY = "scarcity";

  public static final double DEFAULT_MAX_PRODUCTION_VALUE = 15.5;
  public static final double DEFAULT_MAX_RESOURCE_DIVERSITY_VALUE = 3.0;
  public static final double DEFAULT_MAX_NUMBER_DIVERSITY_VALUE = 3.0;
  public static final double DEFAULT_MAX_SCARCITY_VALUE = 23.7;

  public static final double DEFAULT_TARGET_PRODUCTION_RATIO = 5.0;
  public static final double DEFAULT_TARGET_RESOURCE_DIVERSITY_RATIO = 2.0;
  public static final double DEFAULT_TARGET_NUMBER_DIVERSITY_RATIO = 2.0;
  public static final double DEFAULT_TARGET_SCARCITY_RATIO = 1.0;

  private static final double DUPLICATE_NUMBER_PENALTY_PERCENT = 20.0;
  private static final double DEFAULT_NUMBER_MULTIPLIER = 1.0;

  private MathUtil() {
  }

  public static double round2(double x) {
    return Math.round(x * 100.0) / 100.0;
  }

  public static double scaleToRatio(double rawValue, double maxRawValue, double targetRatioValue) {
    if (maxRawValue <= 0) {
      return 0;
    }
    double scaled = (rawValue / maxRawValue) * targetRatioValue;
    if (scaled < 0) {
      return 0;
    }
    if (scaled > targetRatioValue) {
      return targetRatioValue;
    }
    return scaled;
  }

  public static double duplicateNumberPenalty(double weightedProductionValue, double numberMultiplier) {
    return percentage(weightedProductionValue, DUPLICATE_NUMBER_PENALTY_PERCENT) * numberMultiplier;
  }

  public static double resolveNumberMultiplier(java.util.Map<String, Double> numberMultipliers, int number) {
    return numberMultipliers.getOrDefault(String.valueOf(number), DEFAULT_NUMBER_MULTIPLIER);
  }

  public static HeuristicScalingContext buildHeuristicScalingContext(
      java.util.Map<String, Double> maxValues,
      java.util.Map<String, Double> targetShares) {
    return new HeuristicScalingContext(
        maxValues.getOrDefault(PRODUCTION_KEY, DEFAULT_MAX_PRODUCTION_VALUE),
        maxValues.getOrDefault(RESOURCE_DIVERSITY_KEY, DEFAULT_MAX_RESOURCE_DIVERSITY_VALUE),
        maxValues.getOrDefault(NUMBER_DIVERSITY_KEY, DEFAULT_MAX_NUMBER_DIVERSITY_VALUE),
        maxValues.getOrDefault(SCARCITY_KEY, DEFAULT_MAX_SCARCITY_VALUE),
        targetShares.getOrDefault(PRODUCTION_KEY, DEFAULT_TARGET_PRODUCTION_RATIO),
        targetShares.getOrDefault(RESOURCE_DIVERSITY_KEY, DEFAULT_TARGET_RESOURCE_DIVERSITY_RATIO),
        targetShares.getOrDefault(NUMBER_DIVERSITY_KEY, DEFAULT_TARGET_NUMBER_DIVERSITY_RATIO),
        targetShares.getOrDefault(SCARCITY_KEY, DEFAULT_TARGET_SCARCITY_RATIO));
  }

  public static void roundHeuristic(Heuristic heuristic, HeuristicScalingContext scalingContext, int settlementsCount) {
    double p = round2(scaleToRatio(heuristic.getProductionValue(), scalingContext.maxProductionValue() * settlementsCount, scalingContext.productionRatio()));
    double r = round2(scaleToRatio(heuristic.getResourceDiversityValue(), scalingContext.maxResourceDiversityValue() * settlementsCount, scalingContext.resourceDiversityRatio()));
    double n = round2(scaleToRatio(heuristic.getNumberDiversityValue(), scalingContext.maxNumberDiversityValue() * settlementsCount, scalingContext.numberDiversityRatio()));
    double s = round2(scaleToRatio(heuristic.getScarcityValue(), scalingContext.maxScarcityValue() * settlementsCount, scalingContext.scarcityRatio()));
    double balanced = weightedTotalForTactic(Tactic.BALANCED, p, r, n, s);
    double productionFocused = weightedTotalForTactic(Tactic.PRODUCTION_FOCUSED, p, r, n, s);
    double scarcityFocused = weightedTotalForTactic(Tactic.SCARCITY_FOCUSED, p, r, n, s);
    heuristic.setProductionValue(p);
    heuristic.setResourceDiversityValue(r);
    heuristic.setNumberDiversityValue(n);
    heuristic.setScarcityValue(s);
    heuristic.setBalancedValue(round2(balanced));
    heuristic.setProductionFocusedValue(round2(productionFocused));
    heuristic.setScarcityFocusedValue(round2(scarcityFocused));
    heuristic.setOverallValue(round2(p + r + n + s));
  }

  private static double weightedTotalForTactic(Tactic tactic, double p, double r, double n, double s) {
    return p * tactic.getProductionWeight()
        + r * tactic.getResourceDiversityWeight()
        + n * tactic.getNumberDiversityWeight()
        + s * tactic.getScarcityWeight();
  }

  private static double percentage(double value, double percent) {
    return value * percent / 100.0;
  }

  public record HeuristicScalingContext(
      double maxProductionValue,
      double maxResourceDiversityValue,
      double maxNumberDiversityValue,
      double maxScarcityValue,
      double productionRatio,
      double resourceDiversityRatio,
      double numberDiversityRatio,
      double scarcityRatio) {
  }
}
