package com.example.catan.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HeuristicDto {
  private double productionValue;
  private double resourceDiversityValue;
  private double numberDiversityValue;
  private double scarcityValue;
  private double balancedValue;
  private double productionFocusedValue;
  private double scarcityFocusedValue;
  private double overallValue;
}
