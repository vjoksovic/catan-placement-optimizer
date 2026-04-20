package com.example.catan.models.enums;

import java.util.Objects;

public enum HeatmapRating {
  
  VERY_LOW,
  LOW,
  MEDIUM,
  HIGH,
  VERY_HIGH;
  
  public static HeatmapRating fromBoardTotals(
      double vertexTotal, double minTotal, double maxTotal, double[] caps) {
    Objects.requireNonNull(caps, "caps");
    if (caps.length != 4) {
      throw new IllegalArgumentException("heatmapRatingCaps must have length 4");
    }
    if (!Double.isFinite(vertexTotal)) {
      return MEDIUM;
    }
    if (maxTotal <= minTotal + 1e-9) {
      return MEDIUM;
    }
    double t = (vertexTotal - minTotal) / (maxTotal - minTotal);
    t = Math.max(0, Math.min(1, t));
    if (t < caps[0]) {
      return VERY_LOW;
    }
    if (t < caps[1]) {
      return LOW;
    }
    if (t < caps[2]) {
      return MEDIUM;
    }
    if (t < caps[3]) {
      return HIGH;
    }
    return VERY_HIGH;
  }
}
