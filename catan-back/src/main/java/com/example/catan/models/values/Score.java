package com.example.catan.models.values;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Score {
  private int productionScore;
  private int diversityScore;
  private int scarcityScore;
  private int totalScore;

  public Score() {
    this.productionScore = 0;
    this.diversityScore = 0;
    this.scarcityScore = 0;
    this.totalScore = 0;
  }
}
