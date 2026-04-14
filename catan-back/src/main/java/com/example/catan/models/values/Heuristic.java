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
  private double totalValue;

  public Heuristic(double productionValue, double resourceDiversityValue, double numberDiversityValue, double scarcityValue) {
    this.productionValue = productionValue;
    this.resourceDiversityValue = resourceDiversityValue;
    this.numberDiversityValue = numberDiversityValue;
    this.scarcityValue = scarcityValue;
    this.totalValue = productionValue + resourceDiversityValue + numberDiversityValue + scarcityValue;
  }

  public Heuristic() {
    this.productionValue = 0;
    this.resourceDiversityValue = 0;
    this.numberDiversityValue = 0;
    this.scarcityValue = 0;
    this.totalValue = 0;
  }
}
