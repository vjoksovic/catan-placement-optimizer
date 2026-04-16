package com.example.catan.utils;

import com.example.catan.models.enums.Playstyle;
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

  public static double round1(double x) {
    return Math.round(x * 10.0) / 10.0;
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

  public static void roundHeuristicToOneDecimal(
      Heuristic heuristic,
      HeuristicScalingContext scalingContext) {
    double p = round1(scaleToRatio(heuristic.getProductionValue(), scalingContext.maxProductionValue(), scalingContext.productionRatio()));
    double r = round1(scaleToRatio(heuristic.getResourceDiversityValue(), scalingContext.maxResourceDiversityValue(), scalingContext.resourceDiversityRatio()));
    double n = round1(scaleToRatio(heuristic.getNumberDiversityValue(), scalingContext.maxNumberDiversityValue(), scalingContext.numberDiversityRatio()));
    double s = round1(scaleToRatio(heuristic.getScarcityValue(), scalingContext.maxScarcityValue(), scalingContext.scarcityRatio()));
    double balanced = weightedTotalForPlaystyle(Playstyle.BALANCED, p, r, n, s);
    double productionFocused = weightedTotalForPlaystyle(Playstyle.PRODUCTION_FOCUSED, p, r, n, s);
    double scarcityFocused = weightedTotalForPlaystyle(Playstyle.SCARCITY_FOCUSED, p, r, n, s);
    heuristic.setProductionValue(p);
    heuristic.setResourceDiversityValue(r);
    heuristic.setNumberDiversityValue(n);
    heuristic.setScarcityValue(s);
    heuristic.setBalancedValue(round1(balanced));
    heuristic.setProductionFocusedValue(round1(productionFocused));
    heuristic.setScarcityFocusedValue(round1(scarcityFocused));
    heuristic.setOverallValue(round1(p + r + n + s));
  }

  private static double weightedTotalForPlaystyle(Playstyle playstyle, double p, double r, double n, double s) {
    return p * playstyle.getProductionWeight()
        + r * playstyle.getResourceDiversityWeight()
        + n * playstyle.getNumberDiversityWeight()
        + s * playstyle.getScarcityWeight();
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
