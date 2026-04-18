package com.example.catan.models.values;

import com.example.catan.utils.MathUtil;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Score {
  private double productionScore;
  private double diversityScore;
  private double scarcityScore;
  private double totalScore;

  public Score() {
    this.productionScore = 0;
    this.diversityScore = 0;
    this.scarcityScore = 0;
    this.totalScore = 0;
  }

  public void setValues(double productionValue, double resourceDiversityValue, double numberDiversityValue, double scarcityValue) {
    this.productionScore = MathUtil.round2(productionValue);
    this.diversityScore = MathUtil.round2((resourceDiversityValue + numberDiversityValue) / 2);
    this.scarcityScore = MathUtil.round2(scarcityValue);
    this.totalScore = MathUtil.round2(this.productionScore + this.diversityScore + this.scarcityScore);
  }
}
