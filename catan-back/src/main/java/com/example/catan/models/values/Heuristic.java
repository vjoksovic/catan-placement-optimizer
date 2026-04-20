package com.example.catan.models.values;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Heuristic {
  
  private double productionValue;
  private double resourceDiversityValue;
  private double numberDiversityValue;
  private double scarcityValue;
  private double balancedValue;
  private double productionFocusedValue;
  private double scarcityFocusedValue;
  private double overallValue;

  public Heuristic() {
    this.productionValue = 0;
    this.resourceDiversityValue = 0;
    this.numberDiversityValue = 0;
    this.scarcityValue = 0;
    this.overallValue = 0;
    this.balancedValue = 0;
    this.productionFocusedValue = 0;
    this.scarcityFocusedValue = 0;
  }

  public void setValues(double productionValue, double resourceDiversityValue, double numberDiversityValue, double scarcityValue) {
    this.productionValue = productionValue;
    this.resourceDiversityValue = resourceDiversityValue;
    this.numberDiversityValue = numberDiversityValue;
    this.scarcityValue = scarcityValue;
    this.overallValue = productionValue + resourceDiversityValue + numberDiversityValue + scarcityValue;
    this.balancedValue = productionValue + resourceDiversityValue + numberDiversityValue + scarcityValue;
    this.productionFocusedValue = productionValue + resourceDiversityValue + numberDiversityValue + scarcityValue;
    this.scarcityFocusedValue = productionValue + resourceDiversityValue + numberDiversityValue + scarcityValue;
  }

  public Heuristic(double productionValue, double resourceDiversityValue, double numberDiversityValue, double scarcityValue) {
    this.productionValue = productionValue;
    this.resourceDiversityValue = resourceDiversityValue;
    this.numberDiversityValue = numberDiversityValue;
    this.scarcityValue = scarcityValue;
    this.balancedValue = 0;
    this.productionFocusedValue = 0;
    this.scarcityFocusedValue = 0;
    this.overallValue = 0;
  }
}
